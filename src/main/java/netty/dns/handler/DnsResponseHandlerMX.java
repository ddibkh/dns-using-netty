package netty.dns.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
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
    private List< MXResult > listResult = new ArrayList<>();

    public DnsResponseHandlerMX(Class<T> classI)
    {
        super(classI, DnsRecordType.MX);
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
                throw new DnsException(dnsResponse.code().toString());
            else
            {
                for (int i = 0;  i < count; i++) {
                    DnsRecord record = dnsResponse.recordAt(DnsSection.ANSWER, i);
                    //RR record
                    //2byte(preference) hostname
                    if (record.type() == DnsRecordType.MX) {
                        DnsRawRecord raw = (DnsRawRecord) record;
                        ByteBuf content = raw.content();
                        //read 2byte
                        MXResult mxResult = new MXResult();
                        mxResult.setPreference(content.readUnsignedShort());
                        mxResult.setHostname(DefaultDnsRecordDecoder.decodeName(content));
                        listResult.add(mxResult);
                    }
                }

                //sorting by preference (1. preference, 2. hostname string)
                Comparator<MXResult> comparator =
                        Comparator.comparingInt(MXResult::getPreference).thenComparing(MXResult::getHostname);
                listResult = listResult.stream().sorted(comparator).collect(Collectors.toList());
            }
        }
        finally
        {
            channelHandlerContext.close();
        }
    }

    @Override
    public List<MXResult> getResult()
    {
        return listResult;
    }
}
