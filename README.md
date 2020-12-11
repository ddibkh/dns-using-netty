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

## 내부동작
### DnsResolver::resolveDomainBy[Tcp|Udp] 호출
- 기본적으로 netty dns 처리 핸들러는 SimpleChannelInboundHandler 를 사용한다.
- 인터페이스 정의
```
public interface DnsResolver
{
    static short getRandomId()
    {
        return (short)new Random().nextInt(1 << 15);
    }

    <T extends DnsResult > T
    resolveDomainByTcp(String dnsIp, String domainName, RequestType requestType) throws DnsException;
    <T extends DnsResult > T
    resolveDomainByUdp(String dnsIp, String domainName, RequestType requestType) throws DnsException;
}

* dnsIp : DNS 서버 지정. (empty 인 경우 디폴트로 dns server 는 google dns(8.8.8.8) 가 셋팅됨)
* domainName : 조회하고자 하는 domain
* dnsTimeout RequestType (MX, A, TXT, NS).

** 구현체는 DnsResolverImpl 클래스이다.

** RequestType 은 static 으로 미리 정의되어 있다.
@Data
@RequiredArgsConstructor
public class RequestType
{
    public static final RequestType REQUEST_A = new RequestType(DnsRecordType.A);
    public static final RequestType REQUEST_NS = new RequestType(DnsRecordType.NS);
    public static final RequestType REQUEST_MX = new RequestType(DnsRecordType.MX);
    public static final RequestType REQUEST_TXT = new RequestType(DnsRecordType.TXT);
    private final DnsRecordType type;
}
```

### bootstrap bean 생성
  TCP, UDP 프로토콜 별로 MX, NS, TXT, A 레코드 조회에 대한 각각의 bootstrap 를 Bean 으로 등록하여 사용한다.
  EventLoopGroup 은 하나의 EventLoopGroup 으로 공유한다.
  
### TCP 프로토콜로 MX 레코드 조회를 위한 bootstrap 생성
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
### UDP 프로토콜로 MX 레코드 조회를 위한 bootstrap 생성 
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

### BootstrapFactory
```
요청 레코드 타입별 bootstrap 을 리턴해주는 factory 클래스이다.
DnsResolverImpl 클래스 내부에서 BootstrapFactory 를 통해 타입에 맞는 bootstrap 을 리턴받는다.

private final BootstrapFactory bootstrapFactory;
Bootstrap bootstrap = bootstrapFactory.getBootstrapTcp(requestType.getType());
bootstrap.connect
bootstrap.query
getresult
```

#### 조회 결과
```
내부적으로 AttributeKey 를 사용한다.
각 handler 에서 얻은 결과를 ChannelContext 의 AttributeKey 에 저장한다.
AtributeKey 는 아래와 같이 static 으로 생성되어 있다.
// Result 저장 AttributeKey
public final static AttributeKey<DnsResult> RECORD_RESULT = AttributeKey.valueOf("record_result");
// Error 발생시 Error Message 저장 AttributeKey
public final static AttributeKey<String> ERROR_MSG = AttributeKey.valueOf("errormsg");

DnsResolverImpl 에서는 결과를 ChannelContext 의 AttributeKey 를 통해 얻어온다.
DnsResult result = channelContext.attr(RECORD_RESULT).get();
if( result.getRecords().isEmpty() )
    throw new DnsException(channelContext.attr(ERROR_MSG).get());

DnsResolver::resolveDomainBy[Tcp|Udp] 의 리턴타입은 List<DnsResult> 이다.
List<DnsResult> 를 이용한다. (MX 레코드 조회의 경우 preference 에 따라 sorting 된다.)
```
 

### Using (test/java/netty/dns/test 소스코드 참조)
#### TCP MX 레코드 조회
```
@Resource(name="dnsResolverImpl")
private DnsResolverImpl dnsResolverImpl;

DnsResult result = dnsResolverImpl.resolveDomainByTcp("", "aaa.com", RequestType.REQUEST_MX);
System.out.println(result);
```
#### UDP MX 레코드 조회
```
@Resource(name="dnsResolverImpl")
private DnsResolverImpl dnsResolverImpl;

DnsResult result = dnsResolverImpl.resolveDomainByUdp("", "aaa.com", RequestType.REQUEST_MX);
System.out.println(result);
```

