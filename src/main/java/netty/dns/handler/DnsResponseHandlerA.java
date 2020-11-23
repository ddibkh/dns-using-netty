package netty.dns.handler;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
import io.netty.util.NetUtil;
import lombok.Getter;
import netty.dns.result.AResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/*
auther : ddibkh
description : A 레코드 결과 처리 핸들러
reference : https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/dns
 */
public class DnsResponseHandlerA<T extends DnsResponse> extends DnsResponseHandler<T>
{
    @Getter
    private String domainName;
    private List< AResult > listResult = new ArrayList<>();

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
                throw new DnsException(dnsResponse.code().toString());
            else
            {
                for (int i = 0;  i < count; i++) {
                    DnsRecord record = dnsResponse.recordAt(DnsSection.ANSWER, i);
                    if (record.type() == DnsRecordType.A) {
                        //just print the IP after query
                        DnsRawRecord raw = (DnsRawRecord) record;
                        AResult aResult = new AResult();
                        aResult.setIp(NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(raw.content())));
                        listResult.add(aResult);
                    }
                }

                //sorting by ip string
                Comparator<AResult> comparator =
                        Comparator.comparing(AResult::getIp);
                listResult = listResult.stream().sorted(comparator).collect(Collectors.toList());
            }
        }
        finally
        {
            channelHandlerContext.close();
        }
    }

    public List<AResult> getResult()
    {
        return listResult;
    }
}
