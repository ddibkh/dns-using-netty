package netty.dns.handler;

import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.util.AttributeKey;
import lombok.Getter;
import netty.dns.result.DnsResult;

import java.util.List;

public abstract class DnsResponseHandler<T extends DnsResponse> extends SimpleChannelInboundHandler< T >
{
    /*
    DNS 조회 채널의 결과를 받기 위한 선언. AttributeKey 에 결과 값을 저장한다.
    기존의 handler 에서 getResult 를 호출시에 이미 context 가 close 상태이기 때문에
    handler 가 이미 소멸된 경우 null pointer exception 이 발생한다.
     */

    public final static AttributeKey<DnsResult> RECORD_RESULT = AttributeKey.valueOf("record_result");
    public final static AttributeKey<String> ERROR_MSG = AttributeKey.valueOf("errormsg");

    @Getter
    private final DnsRecordType recordType;

    public DnsResponseHandler(Class<T> classI, DnsRecordType recordType)
    {
        super(classI);
        this.recordType = recordType;
    }
}
