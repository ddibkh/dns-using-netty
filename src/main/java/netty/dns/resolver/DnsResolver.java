/*
auther : ddibkh
description : DNS resolver
reference : https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/dns
 */

package netty.dns.resolver;

import netty.dns.exception.DnsException;
import netty.dns.result.DnsResult;

import java.util.List;
import java.util.Random;

public interface DnsResolver
{
    static short getRandomId()
    {
        return (short)new Random().nextInt(1 << 15);
    }

    <T extends DnsResult > T
    resolveDomainByTcp(String dnsIp, String domainName, RequestType requestType) throws DnsException;
    <T extends DnsResult > T
    resolveDomainByUdp(String dnsIp, String domainName, RequestType requestType) throws DnsException;
}
