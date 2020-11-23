# dns-using-netty
dns resolver using netty A, MX, TXT, NS type record

## DnsResolver::resolveDomain 호출
- 기본적으로 netty dns 처리 핸들러는 SimpleChannelInboundHandler 를 사용한다.
```
public < T extends DnsResponseHandler >
    void resolveDoamin(@NonNull String dnsIp,
                       @NonNull Integer dnsTimeout,
                       @NonNull String domainName,
                       @NonNull protocol pt,
                       @NonNull T handler) throws DnsException, InterruptedException

* dnsIp 가 "" 인 경우 디폴트로 dns server 는 google dns(8.8.8.8) 가 셋팅됨.
* dnsTimeout 값이 <=0 인 경우 디폴트로 10초로 셋팅됨.
* protocol
  protocol.TCP : TCP 방식 사용
  protocol.UDP : UDP 방식 사용
```

## handler 생성
  제너릭 파라미터로 DefaultDnsResponse 는 TCP 방식을 사용시 셋팅한다.
  제너릭 파라미터로 DatagramDnsResponse 는 UDP 방식을 사용시 셋팅한다.
1. MX 레코드 조회
```
DnsResponseHandler<[DefaultDnsResponse | DatagramDnsResponse]> handler = 
  new DnsResponseHandlerMX<>([DefaultDnsResponse.class | DatagramDnsResponse.class]);
```
2. A 레코드 조회
```
DnsResponseHandler<[DefaultDnsResponse | DatagramDnsResponse]> handler = 
  new DnsResponseHandlerA<>([DefaultDnsResponse.class | DatagramDnsResponse.class]);
```
3. NS 레코드 조회
```
DnsResponseHandler<[DefaultDnsResponse | DatagramDnsResponse]> handler = 
  new DnsResponseHandlerNS<>([DefaultDnsResponse.class | DatagramDnsResponse.class]);
```
4. TXT 레코드 조회
```
DnsResponseHandler<[DefaultDnsResponse | DatagramDnsResponse]> handler = 
  new DnsResponseHandlerTXT<>([DefaultDnsResponse.class | DatagramDnsResponse.class]);
```

## 조회 결과
resolveDoamin 호출 이후 handler 의 getResult 를 호출
```
List<AResult> result = handler.getResult();      //A record 조회 결과
List<MXResult> result = handler.getResult();     //MX record 조회 결과 (preference 로 정렬됨)
List<NSResult> result = handler.getResult();     //NS record 조회 결과
List<TXTResult> result = handler.getResult();    //TXT record 조회 결과
```
