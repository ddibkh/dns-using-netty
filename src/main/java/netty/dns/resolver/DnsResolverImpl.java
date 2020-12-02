package netty.dns.resolver;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.dns.*;
import lombok.RequiredArgsConstructor;
import netty.dns.configuration.BootstrapFactory;
import netty.dns.exception.DnsException;
import netty.dns.result.DnsResult;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static netty.dns.handler.DnsResponseHandler.ERROR_MSG;
import static netty.dns.handler.DnsResponseHandler.RECORD_RESULT;

@Component("dnsResolverImpl")
@RequiredArgsConstructor
public class DnsResolverImpl implements DnsResolver
{
    private final BootstrapFactory bootstrapFactory;

    @Override
    public DnsResult resolveDomainByTcp(String dnsIp,
                                        String domainName,
                                        RequestType requestType) throws DnsException
    {
        if( dnsIp.isEmpty() )
            dnsIp = "8.8.8.8";

        final int dnsTimeout = 10;

        short randomID = DnsResolver.getRandomId();

        Bootstrap bootstrap = bootstrapFactory.getBootstrapTcp(requestType.getType());

        final Channel ch;
        try
        {
            ch = bootstrap.connect(dnsIp, 53).sync().channel();
        }
        catch( Throwable cte )
        {
            throw new DnsException(
                    String.format("fail to connect dns server, %s", cte.getMessage()));
        }

        DnsQuery query = new DefaultDnsQuery(randomID, DnsOpCode.QUERY)
                .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(domainName, requestType.getType()))
                .setRecursionDesired(true);

        try
        {
            ch.writeAndFlush(query).sync().addListener(
                    future ->
                    {
                        if( !future.isSuccess() )
                            throw new DnsException("fail send query message");
                        else if( future.isCancelled() )
                            throw new DnsException("operation cancelled");
                    }
            );

            boolean bSuccess = ch.closeFuture().await(dnsTimeout, TimeUnit.SECONDS);

            //timeout occured
            if( !bSuccess )
            {
                ch.close().sync();
                throw new DnsException(String.format(
                        "fail to resolve domain by TCP, timed out, domain : %s, dns : %s", domainName, dnsIp));
            }
        }
        catch( InterruptedException ie )
        {
            throw new DnsException("fail to resolve record, interrupted exception");
        }

        //DnsResult result = ch.attr(requestType.getAttributeKey()).get();
        DnsResult result = ch.attr(RECORD_RESULT).get();
        if( result.getRecords().isEmpty() )
            throw new DnsException(ch.attr(ERROR_MSG).get());

        return result;
    }

    @Override
    public DnsResult resolveDomainByUdp(String dnsIp,
                                        String domainName,
                                        RequestType requestType) throws DnsException
    {
        if( dnsIp.isEmpty() )
            dnsIp = "8.8.8.8";

        final int dnsTimeout = 10;

        short randomID = DnsResolver.getRandomId();

        InetSocketAddress addr = new InetSocketAddress(dnsIp, 53);
        Bootstrap bootstrap = bootstrapFactory.getBootstrapUdp(requestType.getType());

        final Channel ch;
        try
        {
            ch = bootstrap.bind(0).sync().channel();

            DnsQuery query = new DatagramDnsQuery(null, addr, randomID)
                    .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(domainName, requestType.getType()))
                    .setRecursionDesired(true);

            ch.writeAndFlush(query).sync().addListener(
                    future ->
                    {
                        if( !future.isSuccess() )
                            throw new DnsException("fail send query message");
                        else if( future.isCancelled() )
                            throw new DnsException("operation cancelled");
                    }
            );

            boolean bSuccess = ch.closeFuture().await(dnsTimeout, TimeUnit.SECONDS);
            if( !bSuccess )
            {
                ch.close().sync();
                throw new DnsException(String.format(
                        "fail to resolve domain by UDP, timed out, domain : %s, dns : %s", domainName, dnsIp));
            }
        }
        catch( InterruptedException ie )
        {
            throw new DnsException("fail to resolve record, interrupted exception");
        }

        DnsResult result = ch.attr(RECORD_RESULT).get();
        if( result.getRecords().isEmpty() )
            throw new DnsException(ch.attr(ERROR_MSG).get());

        return result;
    }
}
