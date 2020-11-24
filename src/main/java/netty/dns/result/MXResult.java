package netty.dns.result;

import lombok.Data;

@Data
public class MXResult extends DnsResult
{
    private final int preference;
    public MXResult(int preference, String record)
    {
        super(DnsResult.Type.MX, record);
        this.preference = preference;
    }

    public String getRecord()
    {
        return super.getRecord();
    }

    public DnsResult.Type getType() { return super.getType(); }

    @Override
    public String toString()
    {
        return "MXResult{" +
                "type=" + getType() +
                ", preference=" + preference +
                ", record='" + getRecord() + '\'' +
                '}';
    }
}
