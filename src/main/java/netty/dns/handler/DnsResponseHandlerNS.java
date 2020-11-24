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
import lombok.extern.slf4j.Slf4j;
import netty.dns.result.DnsResult;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DnsResponseHandlerNS<T extends DnsResponse> extends DnsResponseHandler<T>
{
    @Getter
    private String domainName;
    private List< DnsResult > listResult = new ArrayList<>();

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

        log.error("{}", message);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                T dnsResponse)
    {
        try
        {
            if (dnsResponse.count(DnsSection.QUESTION) > 0) {
                DnsQuestion question = dnsResponse.recordAt(DnsSection.QUESTION, 0);
                log.info("check NS record : {}", question.name());
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
                    if (record.type() == DnsRecordType.NS) {
                        DnsRawRecord raw = (DnsRawRecord) record;
                        DnsResult nsResult = new DnsResult(DnsResult.Type.NS, DefaultDnsRecordDecoder.decodeName(raw.content()));
                        listResult.add(nsResult);
                    }
                }
            }
        }
        finally
        {
            channelHandlerContext.close();
        }
    }

    public List<DnsResult> getResult()
    {
        return listResult;
    }
}
