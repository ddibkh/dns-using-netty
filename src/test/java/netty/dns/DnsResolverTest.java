package netty.dns;

import io.netty.handler.codec.dns.DatagramDnsResponse;
import io.netty.handler.codec.dns.DefaultDnsResponse;
import netty.dns.handler.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class DnsResolverTest
{
    @Test
    void resolveMxRecordTCP()
    {
        try
        {
            DnsResolver dnsResolver = new DnsResolver();
            DnsResponseHandler< DefaultDnsResponse > handler = new DnsResponseHandlerMX<>(DefaultDnsResponse.class);
            //mx record
            dnsResolver.resolveDoamin("",
                    10,
                    "google.com",
                    protocol.TCP,
                    handler
            );

            List results = handler.getResult();
            results.stream().forEach(System.out::println);
        }
        catch( DnsException e )
        {
            System.out.println(e.getMessage());
        }
        catch( InterruptedException ie )
        {
            System.out.println(ie.getMessage());
        }
    }

    @Test
    void resolveNSRecordUDP()
    {
        try
        {
            DnsResolver dnsResolver = new DnsResolver();
            DnsResponseHandler< DatagramDnsResponse > handler = new DnsResponseHandlerNS<>(DatagramDnsResponse.class);
            //ns record
            dnsResolver.resolveDoamin("",
                    10,
                    "google.com",
                    protocol.UDP,
                    handler
            );

            List results = handler.getResult();
            results.stream().forEach(System.out::println);
        }
        catch( DnsException e )
        {
            System.out.println(e.getMessage());
        }
        catch( InterruptedException ie )
        {
            System.out.println(ie.getMessage());
        }
    }

    @Test
    void resolveTXTRecordTCP()
    {
        try
        {
            DnsResolver dnsResolver = new DnsResolver();
            DnsResponseHandler< DefaultDnsResponse > handler = new DnsResponseHandlerTXT<>(DefaultDnsResponse.class);
            //txt record
            dnsResolver.resolveDoamin("",
                    10,
                    "google.com",
                    protocol.TCP,
                    handler
            );

            List results = handler.getResult();
            results.stream().forEach(System.out::println);
        }
        catch( DnsException e )
        {
            System.out.println(e.getMessage());
        }
        catch( InterruptedException ie )
        {
            System.out.println(ie.getMessage());
        }
    }

    @Test
    @DisplayName("resolve txt _dmarc record")
    void resolveTXTRecordDmarcUDP()
    {
        try
        {
            DnsResolver dnsResolver = new DnsResolver();
            DnsResponseHandler< DatagramDnsResponse > handler = new DnsResponseHandlerTXT<>(DatagramDnsResponse.class);
            //txt _dmarc record
            dnsResolver.resolveDoamin("",
                    10,
                    "_dmarc.gmail.com",
                    protocol.UDP,
                    handler
            );

            List results = handler.getResult();
            results.stream().forEach(System.out::println);
        }
        catch( DnsException e )
        {
            System.out.println(e.getMessage());
        }
        catch( InterruptedException ie )
        {
            System.out.println(ie.getMessage());
        }
    }
}