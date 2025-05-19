import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ElGamal {
    private final BigInteger p;
    private final BigInteger g;
    private final BigInteger x;
    private final BigInteger y;
    private final Random rnd = new Random();

    public ElGamal(int bits) {
        this.p = BigInteger.probablePrime(bits, rnd);
        this.g = BigInteger.valueOf(2);
        this.x = new BigInteger(bits - 2, rnd).add(BigInteger.ONE);
        this.y = g.modPow(x, p);
    }

    public ElGamal(BigInteger p, BigInteger g, BigInteger y, BigInteger x) {
        this.p = p;
        this.g = g;
        this.y = y;
        this.x = x;
    }

    public BigInteger getP() { return p; }
    public BigInteger getG() { return g; }
    public BigInteger getY() { return y; }
    public BigInteger getX() { return x; }

    public List<BigInteger[]> encrypt(byte[] data) {
        List<BigInteger[]> ct = new ArrayList<>();
        for (byte bb : data) {
            BigInteger m = BigInteger.valueOf(bb & 0xFF);
            BigInteger k = new BigInteger(p.bitLength() - 1, rnd);
            BigInteger a = g.modPow(k, p);
            BigInteger b = y.modPow(k, p).multiply(m).mod(p);
            ct.add(new BigInteger[]{ a, b });
        }
        return ct;
    }

    public byte[] decrypt(List<BigInteger[]> ct) {
        byte[] res = new byte[ct.size()];
        for (int i = 0; i < ct.size(); i++) {
            BigInteger a = ct.get(i)[0], b = ct.get(i)[1];
            BigInteger s = a.modPow(x, p);
            BigInteger m = b.multiply(s.modInverse(p)).mod(p);
            res[i] = m.mod(BigInteger.valueOf(256)).byteValue();
        }
        return res;
    }
}
