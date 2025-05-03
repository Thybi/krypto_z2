import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class ElGamalApp extends JFrame {
    private ElGamal elgamal;
    private byte[] inputData, outputData;

    private final JTextField pField = new JTextField(40);
    private final JTextField gField = new JTextField(40);
    private final JTextField yField = new JTextField(40);
    private final JTextField xField = new JTextField(40);

    private final JTextArea inputArea  = new JTextArea(10, 40);
    private final JTextArea outputArea = new JTextArea(10, 40);

    public ElGamalApp() {
        initUI();
        setVisible(true);
    }

    private void initUI() {
        setTitle("ElGamal GUI");
        setSize(1300, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10,10));

        add(createKeyPanel(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createTextPanel("Wejście (tekst/Base64)", inputArea),
                createTextPanel("Wyjście (tekst/Base64)", outputArea)
        );
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);

        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createKeyPanel() {
        JPanel p = new JPanel(new GridLayout(5,1,5,5));
        p.setBorder(new TitledBorder("Parametry i klucze ElGamal"));

        p.add(createRow("p (hex):", pField));
        p.add(createRow("g (hex):", gField));
        p.add(createRow("y = g^x mod p (hex):", yField));
        p.add(createRow("x (klucz prywatny) (hex):", xField));

        JButton gen = new JButton("Generuj nowe klucze");
        gen.addActionListener(e -> {
            elgamal = new ElGamal(512); // lub 1024/2048
            pField.setText(elgamal.getP().toString(16));
            gField.setText(elgamal.getG().toString(16));
            yField.setText(elgamal.getY().toString(16));
            xField.setText(elgamal.getX().toString(16));
        });
        p.add(gen);

        return p;
    }

    private JPanel createRow(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(5,5));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JPanel createTextPanel(String title, JTextArea area) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(title));
        area.setFont(new Font("Monospaced",Font.PLAIN,12));
        JScrollPane sc = new JScrollPane(area);
        sc.setPreferredSize(new Dimension(550,300));
        p.add(sc);
        return p;
    }

    private JPanel createButtonPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));
        JButton load = new JButton("Załaduj plik");
        JButton save = new JButton("Zapisz plik");
        JButton encrypt = new JButton("Szyfruj");
        JButton decrypt = new JButton("Odszyfruj");

        load.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
                try {
                    inputData = new FileInputStream(fc.getSelectedFile()).readAllBytes();
                    inputArea.setText(Base64.getEncoder().encodeToString(inputData));
                } catch (Exception ex) { showError(ex); }
            }
        });
        save.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
                try (FileOutputStream fos = new FileOutputStream(fc.getSelectedFile())) {
                    fos.write(outputData);
                } catch (Exception ex) { showError(ex); }
            }
        });

        encrypt.addActionListener(e -> process(true));
        decrypt.addActionListener(e -> process(false));

        p.add(load); p.add(save); p.add(encrypt); p.add(decrypt);
        return p;
    }

    private void process(boolean doEncrypt) {
        try {
            BigInteger p = new BigInteger(pField.getText(),16);
            BigInteger g = new BigInteger(gField.getText(),16);
            BigInteger y = new BigInteger(yField.getText(),16);
            BigInteger x = new BigInteger(xField.getText(),16);
            elgamal = new ElGamal(p,g,y,x);

            byte[] in = doEncrypt
                    ? (inputData!=null ? inputData : inputArea.getText().getBytes(StandardCharsets.UTF_8))
                    : Base64.getDecoder().decode(inputArea.getText());

            if (doEncrypt) {
                List<BigInteger[]> ct = elgamal.encrypt(in);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                for (BigInteger[] pair: ct) {
                    byte[] a = pair[0].toByteArray();
                    byte[] b = pair[1].toByteArray();
                    bout.write((byte)a.length);
                    bout.write(a);
                    bout.write((byte)b.length);
                    bout.write(b);
                }
                outputData = bout.toByteArray();
                outputArea.setText(Base64.getEncoder().encodeToString(outputData));
            } else {
                byte[] all = Base64.getDecoder().decode(inputArea.getText());
                ByteArrayInputStream bin = new ByteArrayInputStream(all);
                List<BigInteger[]> ct = new java.util.ArrayList<>();
                while (bin.available()>0) {
                    int la = bin.read();
                    byte[] a = bin.readNBytes(la);
                    int lb = bin.read();
                    byte[] b = bin.readNBytes(lb);
                    ct.add(new BigInteger[]{ new BigInteger(a), new BigInteger(b) });
                }
                byte[] dec = elgamal.decrypt(ct);
                outputData = dec;
                try {
                    outputArea.setText(new String(dec,StandardCharsets.UTF_8));
                } catch (Exception ex) {
                    outputArea.setText("[binary data]");
                }
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(),"Błąd",JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ElGamalApp::new);
    }
}
