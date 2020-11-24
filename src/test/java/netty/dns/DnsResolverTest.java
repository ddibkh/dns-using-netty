package netty.dns;

import netty.dns.handler.DnsException;
import netty.dns.result.DnsResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest( classes = {
        DnsResolverMX.class,
        DnsResolverTXT.class,
        DnsResolverA.class,
        DnsResolverNS.class,
        DnsConfiguration.class
} )
class DnsResolverTest
{
    @Resource(name="dnsResolverMX")
    private DnsResolver dnsResolverMX;
    @Resource(name="dnsResolverTXT")
    private DnsResolver dnsResolverTXT;
    @Resource(name="dnsResolverA")
    private DnsResolver dnsResolverA;
    @Resource(name="dnsResolverNS")
    private DnsResolver dnsResolverNS;

    @Test
    void mxResolveConfiguration()
    {
        try
        {
            dnsResolverMX.resolveDomainByTcp("","google.com")
                .stream().forEach(System.out::println);

            dnsResolverMX.resolveDomainByUdp("8.8.8.8", "google.com")
                .stream().forEach(System.out::println);
        }
        catch( DnsException de )
        {
            System.out.println(de.getMessage());
        }
        catch( Exception e )
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void txtResolveConfiguration()
    {
        try
        {
            dnsResolverTXT.resolveDomainByTcp("", "google.com")
                    .stream().forEach(System.out::println);

            dnsResolverTXT.resolveDomainByUdp("","google.com")
                    .stream().forEach(System.out::println);
        }
        catch( DnsException de )
        {
            System.out.println(de.getMessage());
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    @Test
    void nsResolveConfiguration()
    {
        try
        {
            dnsResolverNS.resolveDomainByTcp("", "google.com")
                    .stream().forEach(System.out::println);

            dnsResolverNS.resolveDomainByUdp("","google.com")
                    .stream().forEach(System.out::println);
        }
        catch( DnsException de )
        {
            System.out.println(de.getMessage());
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    @Test
    void aResolveConfiguration()
    {
        try
        {
            dnsResolverA.resolveDomainByTcp("", "google.com")
                    .stream().forEach(System.out::println);

            dnsResolverA.resolveDomainByUdp("","google.com")
                    .stream().forEach(System.out::println);
        }
        catch( DnsException de )
        {
            System.out.println(de.getMessage());
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}