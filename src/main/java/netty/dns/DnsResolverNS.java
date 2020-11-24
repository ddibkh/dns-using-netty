/*
auther : ddibkh
description : DNS resolver
reference : https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/dns
 */

package netty.dns;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.dns.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import netty.dns.handler.DnsException;
import netty.dns.handler.DnsResponseHandlerNS;
import netty.dns.result.DnsResult;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component("dnsResolverNS")
@RequiredArgsConstructor
@Slf4j
@Lazy
public class DnsResolverNS implements DnsResolver
{
    private final Bootstrap tcpNSBootstrap;
    private final Bootstrap udpNSBootstrap;

    public List<DnsResult> resolveDomainByTcp(String dnsIp, String domainName) throws DnsException
    {
        final int dnsTimeout = 10;

        if( dnsIp.isEmpty() )
            dnsIp = "8.8.8.8";

        short randomID = DnsResolver.getRandomId();

        final Channel ch;
        try
        {
            ch = tcpNSBootstrap.connect(dnsIp, 53).sync().channel();
        }
        catch( Throwable cte )
        {
            log.error("fail to connect dns server, {}", cte.getMessage());
            throw new DnsException(
                    String.format("fail to connect dns server, %s", cte.getMessage()));
        }

        DnsQuery query = new DefaultDnsQuery(randomID, DnsOpCode.QUERY)
                .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(domainName, DnsRecordType.NS))
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
                log.error("fail to resolve domain by TCP, timed out, domain : {}, dns : {}", domainName, dnsIp);
                ch.close().sync();
                throw new DnsException(String.format(
                        "fail to resolve domain by TCP, timed out, domain : %s, dns : %s", domainName, dnsIp));
            }
        }
        catch( InterruptedException ie )
        {
            log.error("fail to resolve NS record, interrupted exception");
            throw new DnsException("fail to resolve NS record, interrupted exception");
        }

        List< DnsResult > list = ch.pipeline().get(DnsResponseHandlerNS.class).getResult();
        return list.stream()
                .map(a -> new DnsResult(a.getType(), a.getRecord()))
                .collect(Collectors.toList());
    }

    public List<DnsResult> resolveDomainByUdp(String dnsIp, String domainName) throws DnsException
    {
        if( dnsIp.isEmpty() )
            dnsIp = "8.8.8.8";

        final int dnsTimeout = 10;

        short randomID = DnsResolver.getRandomId();

        InetSocketAddress addr = new InetSocketAddress(dnsIp, 53);

        final Channel ch;
        try
        {
            ch = udpNSBootstrap.bind(0).sync().channel();

            DnsQuery query = new DatagramDnsQuery(null, addr, randomID)
                    .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(domainName, DnsRecordType.NS))
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
                log.error("fail to resolve domain by UDP, timed out, domain : {}, dns : {}", domainName, dnsIp);
                ch.close().sync();
                throw new DnsException(String.format(
                        "fail to resolve domain by UDP, timed out, domain : %s, dns : %s", domainName, dnsIp));
            }
        }
        catch( InterruptedException ie )
        {
            log.error("fail to resolve NS record, interrupted exception");
            throw new DnsException("fail to resolve NS record, interrupted exception");
        }

        List<DnsResult> list = ch.pipeline().get(DnsResponseHandlerNS.class).getResult();
        return list.stream()
                .map(a -> new DnsResult(a.getType(), a.getRecord()))
                .collect(Collectors.toList());
    }
}
