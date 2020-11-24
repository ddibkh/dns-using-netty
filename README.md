# dns-using-netty
dns resolver using netty A, MX, TXT, NS type record

# dependency
```
<parent>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-parent</artifactId>
	<version>2.3.4.RELEASE</version>
	<relativePath/> <!-- lookup parent from repository -->
</parent>
    
<dependency>
	<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter</artifactId>
	</dependency>

	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-test</artifactId>
		<scope>test</scope>
	</dependency>

	<dependency>
		<groupId>org.projectlombok</groupId>
		<artifactId>lombok</artifactId>
		<optional>true</optional>
	</dependency>

	<dependency>
		<groupId>io.netty</groupId>
		<artifactId>netty-all</artifactId>
		<version>4.1.53.Final</version>
</dependency>
```

## DnsResolver::resolveDomainBy[Tcp|Udp] 호출
- 기본적으로 netty dns 처리 핸들러는 SimpleChannelInboundHandler 를 사용한다.
- 인터페이스 정의
```
public interface DnsResolver
{
    static short getRandomId()
    {
        return (short)new Random().nextInt(1 << 15);
    }

    <T extends DnsResult > List< T >
    resolveDomainByTcp(String dnsServer, String domainName) throws DnsException;
    <T extends DnsResult> List< T >
    resolveDomainByUdp(String dnsServer, String domainName) throws DnsException;
}

* dnsIp : DNS 서버 지정. (empty 인 경우 디폴트로 dns server 는 google dns(8.8.8.8) 가 셋팅됨)
* domainName : 조회하고자 하는 domain
* dnsTimeout 값이 디폴트로 10초로 사용함.
```

## bootstrap bean 생성
  TCP, UDP 프로토콜 별로 MX, NS, TXT, A 레코드 조회에 대한 각각의 bootstrap 를 Bean 으로 등록하여 사용한다.
  EventLoopGroup 은 하나의 EventLoopGroup 으로 공유한다.
  
# TCP 프로토콜로 MX 레코드 조회를 위한 bootstrap 생성
```
@Bean
@Lazy
public Bootstrap tcpMxBootstrap()
{
    //default timeout 10
    Bootstrap b = new Bootstrap();
    b.group(eventLoopGroup());
    b.channel(NioSocketChannel.class);
    b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
    b.handler(new ChannelInitializer< SocketChannel >()
    {
        @Override
        protected void initChannel(SocketChannel socketChannel)
        {
            ChannelPipeline p = socketChannel.pipeline();

            //tcp protocol
            p.addLast(new ReadTimeoutHandler(10))
                .addLast(new WriteTimeoutHandler(10))
                .addLast(new TcpDnsQueryEncoder())
                .addLast(new TcpDnsResponseDecoder())
                .addLast(new DnsResponseHandlerMX<>(DefaultDnsResponse.class));
        }
    });
    return b;
}
```
# UDP 프로토콜로 MX 레코드 조회를 위한 bootstrap 생성 
```
@Bean
@Lazy
public Bootstrap udpMxBootstrap()
{
    //UDP 의 경우 timeout 처리는 resolver 내에서 처리한다.
    Bootstrap b = new Bootstrap();
    b.group(eventLoopGroup());
    b.channel(NioDatagramChannel.class);
    b.handler(new ChannelInitializer< DatagramChannel >()
    {
        @Override
        protected void initChannel(DatagramChannel socketChannel)
        {
            ChannelPipeline p = socketChannel.pipeline();

            p.addLast(new DatagramDnsQueryEncoder())
                .addLast(new DatagramDnsResponseDecoder())
                .addLast(new DnsResponseHandlerMX<>(DatagramDnsResponse.class));
        }
    });
    return b;
}
```

## 각 bootstrap bean 을 주입하여 사용한다.
```
@Resource(name="dnsResolverMX")
private DnsResolver dnsResolverMX;
@Resource(name="dnsResolverTXT")
private DnsResolver dnsResolverTXT;
@Resource(name="dnsResolverA")
private DnsResolver dnsResolverA;
@Resource(name="dnsResolverNS")
private DnsResolver dnsResolverNS;

혹은 생성자 주입.
```
# 조회 결과
DnsResolver::resolveDomainBy[Tcp|Udp] 의 리턴타입은 List<DnsResult> 이다.
List<DnsResult> 를 이용한다. (MX 레코드 조회의 경우 preference 값을 사용하려면
List<MXRecord> 로 결과를 받아 사용한다.)
 

## example (DnsResolverTest.java 참조)
# TCP MX 레코드 조회
```
@Resource(name="dnsResolverMX")
private DnsResolver dnsResolverMX;

@Test
void mxResolveConfiguration()
{
    try
    {
        dnsResolverMX.resolveDomainByTcp("","google.com")
                .stream().forEach(System.out::println);
    }
    catch( DnsException de )
    {
        System.out.println(de.getMessage());
    }
    catch( Exception e )
    {
        System.out.println(e.getMessage());
        e.printStackTrace();
    }
}
```
# UDP MX 레코드 조회
```
@Resource(name="dnsResolverMX")
private DnsResolver dnsResolverMX;

@Test
void mxResolveConfiguration()
{
    try
    {
        dnsResolverMX.resolveDomainByUdp("","google.com")
                .stream().forEach(System.out::println);
    }
    catch( DnsException de )
    {
        System.out.println(de.getMessage());
    }
    catch( Exception e )
    {
        System.out.println(e.getMessage());
        e.printStackTrace();
    }
}
```

# 중복코드 제거를 위한 리팩토링 필요.
