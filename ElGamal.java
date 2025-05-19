import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ElGamal {
    private final BigInteger p;
    private final BigInteger g;
    private final BigInteger a;
    private final BigInteger h;
    private final Random rnd = new Random();

    public ElGamal(int bits) {
        this.p = BigInteger.probablePrime(bits, rnd);
        this.g = BigInteger.valueOf(2);
        this.a = new BigInteger(bits - 2, rnd).add(BigInteger.ONE);
        this.h = g.modPow(a, p);
    }

    public ElGamal(BigInteger p, BigInteger g, BigInteger h, BigInteger a) {
        this.p = p;
        this.g = g;
        this.h = h;
        this.a = a;
    }

    public BigInteger getP() { return p; }
    public BigInteger getG() { return g; }
    public BigInteger getH() { return h; }
    public BigInteger getA() { return a; }

    public List<BigInteger[]> encrypt(byte[] data) {
        List<BigInteger[]> ct = new ArrayList<>();
        for (byte bb : data) {
            BigInteger m = BigInteger.valueOf(bb & 0xFF);
            BigInteger k = new BigInteger(p.bitLength() - 1, rnd);
            BigInteger alpha = g.modPow(k, p);
            BigInteger beta = h.modPow(k, p).multiply(m).mod(p);
            ct.add(new BigInteger[]{ alpha, beta });
        }
        return ct;
    }

    public byte[] decrypt(List<BigInteger[]> ct) {
        byte[] res = new byte[ct.size()];
        for (int i = 0; i < ct.size(); i++) {
            BigInteger alpha = ct.get(i)[0], beta = ct.get(i)[1];
            BigInteger s = alpha.modPow(a, p);
            BigInteger m = beta.multiply(s.modInverse(p)).mod(p);
            res[i] = m.mod(BigInteger.valueOf(256)).byteValue();
        }
        return res;
    }
}