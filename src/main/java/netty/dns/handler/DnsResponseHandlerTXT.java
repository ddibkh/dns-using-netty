package netty.dns.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import netty.dns.result.DnsResult;

import java.util.ArrayList;
import java.util.List;

/*
auther : ddibkh
description : TXT 레코드 결과 처리 핸들러
reference : https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/dns
 */
@Slf4j
public class DnsResponseHandlerTXT<T extends DnsResponse> extends DnsResponseHandler<T>
{
    @Getter
    String domainName;
    private List< DnsResult > listResult = new ArrayList<>();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        String message;
        if( cause instanceof ReadTimeoutException )
            message = "TXT handler read timed out";
        else if( cause instanceof WriteTimeoutException )
            message = "TXT handler write timed out";
        else
            message = String.format("TXT handler exception caught, %s", cause.getMessage());

        log.error("{}", message);
        ctx.close();
    }

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
                log.info("check TXT record : {}", question.name());
                domainName = question.name();
            }
            else
                domainName = "";

            int count = dnsResponse.count(DnsSection.ANSWER);
            log.debug("TXT record answer count : {}", count);

            //error
            if( count == 0 )
            {
                log.error("fail to TXT record domain '{}', {}", domainName, dnsResponse.code().toString());
                throw new DnsException(dnsResponse.code().toString());
            }
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
                        //read 2byte
                        DnsResult txtResult = new DnsResult(DnsResult.Type.TXT, sb.toString());
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
    public List<DnsResult> getResult()
    {
        return listResult;
    }
}
