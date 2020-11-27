package netty.dns.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import lombok.Getter;
import netty.dns.result.MXResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/*
auther : ddibkh
description : MX 레코드 결과 처리 핸들러
reference : https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/dns
 */
public class DnsResponseHandlerMX<T extends DnsResponse> extends DnsResponseHandler<T>
{
    @Getter
    private String domainName;

    public DnsResponseHandlerMX(Class<T> classI)
    {
        super(classI, DnsRecordType.MX);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        String message;
        if( cause instanceof ReadTimeoutException )
            message = "MX handler read timed out";
        else if( cause instanceof WriteTimeoutException )
            message = "MX handler write timed out";
        else
            message = String.format("MX handler exception caught, %s", cause.getMessage());

        ctx.close();
        throw new DnsException(message);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                T dnsResponse) throws DnsException
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
                List<MXResult> results = new ArrayList<>();
                for (int i = 0;  i < count; i++)
                {
                    DnsRecord record = dnsResponse.recordAt(DnsSection.ANSWER, i);
                    if (record.type() == DnsRecordType.MX) {
                        //just print the IP after query
                        DnsRawRecord raw = (DnsRawRecord) record;
                        ByteBuf content = raw.content();
                        //preference(2bytes) hostname
                        MXResult mxResult = new MXResult(
                                content.readUnsignedShort(), DefaultDnsRecordDecoder.decodeName(content));
                        results.add(mxResult);
                    }
                }

                //sorting by preference (1. preference, 2. hostname)
                Comparator< MXResult > comparator =
                        Comparator.comparingInt(MXResult::getPreference).thenComparing(MXResult::getRecord);
                results = results.stream().sorted(comparator).collect(Collectors.toList());
                channelHandlerContext.channel().attr(MX_RECORD_RESULT).set(results);
            }
        }
        finally
        {
            channelHandlerContext.close();
        }
    }
}
