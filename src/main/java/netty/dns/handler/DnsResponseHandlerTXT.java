package netty.dns.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
import lombok.Getter;
import netty.dns.result.TXTResult;

import java.util.ArrayList;
import java.util.List;

/*
auther : ddibkh
description : TXT 레코드 결과 처리 핸들러
reference : https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/dns
 */
public class DnsResponseHandlerTXT<T extends DnsResponse> extends DnsResponseHandler<T>
{
    @Getter
    String domainName;
    private List< TXTResult > listResult = new ArrayList<>();

    public DnsResponseHandlerTXT(Class<T> classI)
    {
        super(classI, DnsRecordType.TXT);
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
                    if (record.type() == DnsRecordType.TXT)
                    {
                        DnsRawRecord raw = (DnsRawRecord) record;
                        ByteBuf content = raw.content();
                        StringBuilder sb = new StringBuilder();
                        while( content.readableBytes() > 0 )
                        {
                            //get record length (2byte)
                            int readLen = content.readUnsignedByte();
                            byte[] bytes = new byte[readLen];
                            ByteBuf bb = content.readBytes(readLen);
                            bb.readBytes(bytes);
                            sb.append(new String(bytes));
                        }

                        TXTResult txtResult = new TXTResult();
                        txtResult.setRecord(sb.toString());
                        listResult.add(txtResult);
                    }
                }
            }
        }
        finally
        {
            channelHandlerContext.close();
        }
    }

    @Override
    public List<TXTResult> getResult()
    {
        return listResult;
    }
}
