/*
auther : ddibkh
description : DNS resolver
reference : https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/dns
 */

package netty.dns;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.dns.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import netty.dns.handler.DnsException;
import netty.dns.handler.DnsResponseHandler;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

enum protocol {TCP, UDP}

@RequiredArgsConstructor
public class DnsResolver
{
    private  < T extends DnsResponseHandler >
    Bootstrap makeBootstrap(@NonNull EventLoopGroup group, protocol pt, T handler)
    {
        Bootstrap b = new Bootstrap();
        b.group(group);

        if( pt == protocol.TCP )
        {
            b.channel(NioSocketChannel.class);
            b.handler(
                new ChannelInitializer< SocketChannel >()
                {
                    @Override
                    protected void initChannel(SocketChannel socketChannel)
                    {
                        ChannelPipeline p = socketChannel.pipeline();

                        //tcp protocol
                        p.addLast(new TcpDnsQueryEncoder())
                                .addLast(new TcpDnsResponseDecoder());

                        p.addLast(handler);
                    }
                }
            );
        }
        //UDP protocol
        else
        {
            b.channel(NioDatagramChannel.class);
            b.handler(
                new ChannelInitializer< DatagramChannel >()
                {
                    @Override
                    protected void initChannel(DatagramChannel datagramChannel)
                    {
                        ChannelPipeline p = datagramChannel.pipeline();
                        p.addLast(new DatagramDnsQueryEncoder())
                                .addLast(new DatagramDnsResponseDecoder());

                        p.addLast(handler);
                    }
                }
            );
        }


        return b;
    }

    public < T extends DnsResponseHandler >
    void resolveDoamin(@NonNull String dnsIp,
                       @NonNull Integer dnsTimeout,
                       @NonNull String domainName,
                       @NonNull protocol pt,
                       @NonNull T handler) throws DnsException, InterruptedException
    {
        if( dnsIp.isEmpty() )
            dnsIp = "8.8.8.8";

        if( dnsTimeout <= 0 )
            dnsTimeout = 10;

        EventLoopGroup group = new NioEventLoopGroup();

        Bootstrap bootstrap = makeBootstrap(group, pt, handler);

        try
        {
            short randomID = (short)new Random().nextInt(1 << 15);
            if( pt == protocol.TCP )
            {
                final Channel ch = bootstrap.connect(dnsIp, 53).sync().channel();
                //int randomID = new Random().nextInt(60000 - 1000) + 1000;
                DnsQuery query = new DefaultDnsQuery(randomID, DnsOpCode.QUERY)
                        .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(domainName, handler.getRecordType()))
                        .setRecursionDesired(true);
                ch.writeAndFlush(query).sync();
                boolean bSuccess = ch.closeFuture().await(dnsTimeout, TimeUnit.SECONDS);

                //timeout occured
                if( !bSuccess )
                {
                    ch.close().sync();
                    throw new DnsException(String.format(
                            "fail to resolve domain by TCP, timed out, domain : %s, dns : %s", domainName, dnsIp)
                    );
                }
            }
            else
            {
                InetSocketAddress addr = new InetSocketAddress(dnsIp, 53);
                final Channel ch = bootstrap.bind(0).sync().channel();
                DnsQuery query = new DatagramDnsQuery(null, addr, randomID)
                        .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(domainName, handler.getRecordType()))
                        .setRecursionDesired(true);
                ch.writeAndFlush(query).sync();
                boolean bSuccess = ch.closeFuture().await(dnsTimeout, TimeUnit.SECONDS);
                if( !bSuccess )
                {
                    ch.close().sync();
                    throw new DnsException(String.format(
                            "fail to resolve domain by UDP, timed out, domain : %s, dns : %s", domainName, dnsIp)
                    );
                }
            }
        }
        finally
        {
            group.shutdownGracefully();
        }

        System.out.println("end resolveDomain");
    }
}
