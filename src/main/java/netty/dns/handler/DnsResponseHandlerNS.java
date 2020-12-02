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
import netty.dns.exception.DnsException;
import netty.dns.result.DnsResult;

import java.util.ArrayList;
import java.util.Collections;
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

        DnsResult dnsResult = new DnsResult(DnsResult.Type.NS, domainName, Collections.emptyList());
        ctx.channel().attr(RECORD_RESULT).set(dnsResult);
        ctx.channel().attr(ERROR_MSG).set(message);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                T dnsResponse)
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
            List<String> results = new ArrayList<>();
            for (int i = 0;  i < count; i++)
            {
                DnsRecord nsrecord = dnsResponse.recordAt(DnsSection.ANSWER, i);
                if (nsrecord.type() == DnsRecordType.NS) {
                    DnsRawRecord raw = (DnsRawRecord) nsrecord;
                    String record = DefaultDnsRecordDecoder.decodeName(raw.content());
                    results.add(record);
                }
            }

            DnsResult nsResult = new DnsResult(DnsResult.Type.NS, domainName, results);
            channelHandlerContext.channel().attr(RECORD_RESULT).set(nsResult);
        }

        channelHandlerContext.close();
    }
}
