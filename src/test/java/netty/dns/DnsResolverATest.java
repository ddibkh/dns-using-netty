package netty.dns;

import netty.dns.configuration.BootstrapFactory;
import netty.dns.configuration.DnsConfiguration;
import netty.dns.exception.DnsException;
import netty.dns.resolver.DnsResolver;
import netty.dns.resolver.DnsResolverImpl;
import netty.dns.resolver.RequestType;
import netty.dns.result.DnsResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@SpringBootTest( classes = {
        DnsConfiguration.class,
        BootstrapFactory.class,
        DnsResolver.class,
        DnsResolverImpl.class
} )

class DnsResolverATest
{
    @Resource(name="dnsResolverImpl")
    private DnsResolverImpl dnsResolverImpl;

    @Test
    void dnsResolveAsync()
    {
        List< CompletableFuture< Void > > listFuture = new ArrayList<>();
        for( int i = 0; i < 3; i++ )
        {
            String domainName;
            if( i % 3 == 0 )
                domainName = "test-domain-1";
            else if( i % 3 == 1 )
                domainName = "test-domain-2";
            else
                domainName = "test-domain-3";

            CompletableFuture< Void > completableFuture = CompletableFuture.supplyAsync(() ->
                    dnsResolverImpl.resolveDomainByTcp("", domainName, RequestType.REQUEST_A))
                    .exceptionally(throwable ->
                    {
                        System.out.println("exceptionally : " + throwable.getMessage());
                        return new DnsResult(DnsResult.Type.A, domainName, Collections.emptyList());
                    })
                    .thenAccept(System.out::println);

            listFuture.add(completableFuture);
        }

        try
        {
            listFuture.stream().map(CompletableFuture::join).collect(Collectors.toList());
        }
        catch( CompletionException ce )
        {
            System.out.println("ce : " + ce.getMessage());
        }
        catch( DnsException de )
        {
            System.out.println("de : " + de.getMessage());
        }
    }


    @Test
    void dnsResolveSync()
    {
        for( int i = 0; i < 3; i++ )
        {
            String domainName;
            if( i % 3 == 0 )
                domainName = "test-domain-1";
            else if( i % 3 == 1 )
                domainName = "test-domain-2";
            else
                domainName = "test-domain-3";

            DnsResult result = dnsResolverImpl.resolveDomainByUdp("", domainName, RequestType.REQUEST_A);
            System.out.println(result);
        }
    }
}
