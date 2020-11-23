package netty.dns.handler;

import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import lombok.Getter;

public abstract class DnsResponseHandler<T extends DnsResponse> extends SimpleChannelInboundHandler< T >
{
    @Getter
    private final DnsRecordType recordType;

    public DnsResponseHandler(Class<T> classI, DnsRecordType recordType)
    {
        super(classI);
        this.recordType = recordType;
    }

    public abstract <T> T getResult();
}
