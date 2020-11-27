/*
auther : ddibkh
description : A 레코드 결과 처리 핸들러
reference : https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/dns
*/

package netty.dns.handler;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.NetUtil;
import lombok.Getter;
import netty.dns.result.DnsResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DnsResponseHandlerA<T extends DnsResponse> extends DnsResponseHandler<T>
{
    @Getter
    private String domainName;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        String message;
        if( cause instanceof ReadTimeoutException )
            message = "A handler read timed out";
        else if( cause instanceof WriteTimeoutException )
            message = "A handler write timed out";
        else
            message = String.format("A handler exception caught, %s", cause.getMessage());

        ctx.close();
        throw new DnsException(message);
    }

    public DnsResponseHandlerA(Class<T> classI)
    {
        super(classI, DnsRecordType.A);
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
                    if (record.type() == DnsRecordType.A) {
                        DnsRawRecord raw = (DnsRawRecord) record;
                        DnsResult aResult = new DnsResult(DnsResult.Type.A,
                                NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(raw.content())));
                        results.add(aResult);
                    }
                }

                //sorting by preference
                Comparator< DnsResult > comparator =
                        Comparator.comparing(DnsResult::getRecord);
                results = results.stream().sorted(comparator).collect(Collectors.toList());
                channelHandlerContext.channel().attr(A_RECORD_RESULT).set(results);
            }
        }
        finally
        {
            channelHandlerContext.close();
        }
    }
}
