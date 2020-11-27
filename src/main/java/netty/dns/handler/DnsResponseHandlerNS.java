/*
auther : ddibkh
description : NS 레코드 결과 처리 핸들러
reference : https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/dns
 */

package netty.dns.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import lombok.Getter;
import netty.dns.result.DnsResult;

import java.util.ArrayList;
import java.util.List;

public class DnsResponseHandlerNS<T extends DnsResponse> extends DnsResponseHandler<T>
{
    @Getter
    private String domainName;

    public DnsResponseHandlerNS(Class<T> classI)
    {
        super(classI, DnsRecordType.NS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        String message;
        if( cause instanceof ReadTimeoutException )
            message = "NS handler read timed out";
        else if( cause instanceof WriteTimeoutException )
            message = "NS handler write timed out";
        else
            message = String.format("NS handler exception caught, %s", cause.getMessage());

        ctx.close();
        throw new DnsException(message);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                T dnsResponse)
    {
        try
        {
            if (dnsResponse.count(DnsSection.QUESTION) > 0) {
                DnsQuestion question = dnsResponse.recordAt(DnsSection.QUESTION, 0);
                domainName = question.name();
            }
            else
                domainName = "";

            int count = dnsResponse.count(DnsSection.ANSWER);

            //error
            if( count == 0 )
            {
                throw new DnsException(dnsResponse.code().toString());
            }
            else
            {
                List<DnsResult> results = new ArrayList<>();
                for (int i = 0;  i < count; i++)
                {
                    DnsRecord record = dnsResponse.recordAt(DnsSection.ANSWER, i);
                    if (record.type() == DnsRecordType.NS) {
                        DnsRawRecord raw = (DnsRawRecord) record;
                        DnsResult nsResult = new DnsResult(DnsResult.Type.NS, DefaultDnsRecordDecoder.decodeName(raw.content()));
                        results.add(nsResult);
                    }
                }

                channelHandlerContext.channel().attr(NS_RECORD_RESULT).set(results);
            }
        }
        finally
        {
            channelHandlerContext.close();
        }
    }
}
