package netty.dns.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.dns.*;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import lombok.Getter;
import netty.dns.exception.DnsException;
import netty.dns.result.DnsResult;

import java.util.ArrayList;
import java.util.Collections;
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

        DnsResult dnsResult = new DnsResult(DnsResult.Type.TXT, domainName, Collections.emptyList());
        ctx.channel().attr(RECORD_RESULT).set(dnsResult);
        ctx.channel().attr(ERROR_MSG).set(message);
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
            List<String> results = new ArrayList<>();
            for (int i = 0;  i < count; i++)
            {
                DnsRecord txtrecord = dnsResponse.recordAt(DnsSection.ANSWER, i);
                if (txtrecord.type() == DnsRecordType.TXT)
                {
                    DnsRawRecord raw = (DnsRawRecord) txtrecord;
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

                    results.add(sb.toString());
                }
            }

            DnsResult txtResult = new DnsResult(DnsResult.Type.TXT, domainName, results);
            channelHandlerContext.channel().attr(RECORD_RESULT).set(txtResult);
        }

        channelHandlerContext.close();
    }
}
