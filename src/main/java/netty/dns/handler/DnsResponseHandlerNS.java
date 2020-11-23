package netty.dns.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
import lombok.Getter;
import netty.dns.result.NSResult;

import java.util.ArrayList;
import java.util.List;

/*
auther : ddibkh
description : NS 레코드 결과 처리 핸들러
reference : https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/dns
 */
public class DnsResponseHandlerNS<T extends DnsResponse> extends DnsResponseHandler<T>
{
    @Getter
    private String domainName;
    private List< NSResult > listResult = new ArrayList<>();

    public DnsResponseHandlerNS(Class<T> classI)
    {
        super(classI, DnsRecordType.NS);
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
                throw new DnsException(dnsResponse.code().toString());
            else
            {
                for (int i = 0;  i < count; i++) {
                    DnsRecord record = dnsResponse.recordAt(DnsSection.ANSWER, i);
                    if (record.type() == DnsRecordType.NS) {
                        DnsRawRecord raw = (DnsRawRecord) record;
                        NSResult nsResult = new NSResult();
                        nsResult.setNsName(DefaultDnsRecordDecoder.decodeName(raw.content()));
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

    public List<NSResult> getResult()
    {
        return listResult;
    }
}
