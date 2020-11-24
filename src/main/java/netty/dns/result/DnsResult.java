package netty.dns.result;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class DnsResult
{
    public enum Type {MX, A, TXT, NS}

    private final Type type;
    private final String record;
}
