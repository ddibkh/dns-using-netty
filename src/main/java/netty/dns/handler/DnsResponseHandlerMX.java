package netty.dns.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import lombok.Getter;
import netty.dns.exception.DnsException;
import netty.dns.result.DnsResult;

import java.util.*;
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

        DnsResult dnsResult = new DnsResult(DnsResult.Type.MX, domainName, Collections.emptyList());
        ctx.channel().attr(RECORD_RESULT).set(dnsResult);
        ctx.channel().attr(ERROR_MSG).set(message);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                T dnsResponse) throws DnsException
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
            Comparator<Integer> comparator = Comparator.comparingInt(i -> i);
            Map<Integer, List<String> > map = new TreeMap<>(comparator);
            //List<MXResult> results = new ArrayList<>();
            List<String> results;
            for (int i = 0;  i < count; i++)
            {
                DnsRecord mxrecord = dnsResponse.recordAt(DnsSection.ANSWER, i);
                if (mxrecord.type() == DnsRecordType.MX) {
                    //just print the IP after query
                    DnsRawRecord raw = (DnsRawRecord) mxrecord;
                    ByteBuf content = raw.content();
                    //preference(2bytes) hostname
                    Integer preference = content.readUnsignedShort();
                    String record = DefaultDnsRecordDecoder.decodeName(content);

                    if( map.containsKey(preference) )
                    {
                        map.get(preference).add(record);
                    }
                    else
                    {
                        List<String> list = new ArrayList<>();
                        list.add(record);
                        map.put(preference, list);
                    }
                }
            }

            results = map.entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.toList());

            DnsResult mxResult = new DnsResult(DnsResult.Type.MX, domainName, results);
            channelHandlerContext.channel().attr(RECORD_RESULT).set(mxResult);
        }

        channelHandlerContext.close();
    }
}
