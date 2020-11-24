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
import lombok.extern.slf4j.Slf4j;
import netty.dns.result.DnsResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DnsResponseHandlerA<T extends DnsResponse> extends DnsResponseHandler<T>
{
    @Getter
    private String domainName;
    private List< DnsResult > listResult = new ArrayList<>();

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

        log.error("{}", message);
        ctx.close();
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
                log.info("check A record : {}", question.name());
                domainName = question.name();
            }
            else
                domainName = "";

            int count = dnsResponse.count(DnsSection.ANSWER);
            log.debug("A record answer count : {}", count);

            //error
            if( count == 0 )
            {
                log.error("fail to A record domain '{}', {}", domainName, dnsResponse.code().toString());
                throw new DnsException(dnsResponse.code().toString());
            }
            else
            {
                for (int i = 0;  i < count; i++) {
                    DnsRecord record = dnsResponse.recordAt(DnsSection.ANSWER, i);
                    if (record.type() == DnsRecordType.A) {
                        DnsRawRecord raw = (DnsRawRecord) record;
                        DnsResult aResult = new DnsResult(DnsResult.Type.A,
                                NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(raw.content())));
                        listResult.add(aResult);
                    }
                }

                //sorting by preference
                Comparator< DnsResult > comparator =
                        Comparator.comparing(DnsResult::getRecord);
                listResult = listResult.stream().sorted(comparator).collect(Collectors.toList());
            }
        }
        finally
        {
            channelHandlerContext.close();
        }
    }

    public List< DnsResult > getResult()
    {
        return listResult;
    }
}
