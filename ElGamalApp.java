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
    private final JTextField aField = new JTextField(40);
    private final JTextField hField = new JTextField(40);

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
                createTextPanel("Wejście", inputArea),
                createTextPanel("Wyjście", outputArea)
        );
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);

        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createKeyPanel() {
        JPanel panel = new JPanel(new GridLayout(5,1,5,5));
        panel.setBorder(new TitledBorder("Parametry i klucze ElGamal"));

        panel.add(createRow("p (hex):", pField));
        panel.add(createRow("g (hex):", gField));
        panel.add(createRow("a (klucz prywatny) (hex):", aField));
        panel.add(createRow("h = g^a mod p (hex):", hField));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton gen = new JButton("Generuj nowe klucze");
        gen.addActionListener(e -> {
            elgamal = new ElGamal(512); // lub 1024/2048
            pField.setText(elgamal.getP().toString(16));
            gField.setText(elgamal.getG().toString(16));
            aField.setText(elgamal.getA().toString(16));
            hField.setText(elgamal.getH().toString(16));
        });
        buttonPanel.add(gen);

        JButton verify = new JButton("Weryfikuj klucze");
        verify.addActionListener(e -> verifyKeys());
        buttonPanel.add(verify);

        JButton calculateH = new JButton("Oblicz h");
        calculateH.addActionListener(e -> {
            try {
                BigInteger p = new BigInteger(pField.getText().trim(), 16);
                BigInteger g = new BigInteger(gField.getText().trim(), 16);
                BigInteger a = new BigInteger(aField.getText().trim(), 16);

                BigInteger calculatedH = g.modPow(a, p);
                hField.setText(calculatedH.toString(16));

                JOptionPane.showMessageDialog(this,
                        "Wartość h została obliczona i zaktualizowana.",
                        "Obliczanie h",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                showError(ex);
            }
        });
        buttonPanel.add(calculateH);

        panel.add(buttonPanel);

        return panel;
    }

    private boolean verifyKeys() {
        try {
            BigInteger p = new BigInteger(pField.getText().trim(), 16);
            BigInteger g = new BigInteger(gField.getText().trim(), 16);
            BigInteger a = new BigInteger(aField.getText().trim(), 16);
            BigInteger h = new BigInteger(hField.getText().trim(), 16);

            BigInteger calculatedH = g.modPow(a, p);

            if (!calculatedH.equals(h)) {
                JOptionPane.showMessageDialog(this,
                        "Klucze nie spełniają warunku h = g^a mod p!\n" +
                                "Oczekiwana wartość h (hex): " + calculatedH.toString(16) + "\n" +
                                "Wprowadzona wartość h (hex): " + h.toString(16),
                        "Błąd weryfikacji kluczy",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            elgamal = new ElGamal(p, g, h, a);
            JOptionPane.showMessageDialog(this,
                    "Klucze są poprawne i zostały załadowane.",
                    "Weryfikacja kluczy",
                    JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (Exception ex) {
            showError(ex);
            return false;
        }
    }

    private JPanel createRow(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(5,5));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JPanel createTextPanel(String title, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder(title));
        area.setFont(new Font("Monospaced",Font.PLAIN,12));
        JScrollPane sc = new JScrollPane(area);
        sc.setPreferredSize(new Dimension(550,300));
        panel.add(sc);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER,10,10));
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

        panel.add(load); panel.add(save); panel.add(encrypt); panel.add(decrypt);
        return panel;
    }

    private void process(boolean doEncrypt) {
        try {
            if (elgamal == null) {
                if (!verifyKeys()) {
                    return;
                }
            }

            byte[] in = doEncrypt
                    ? (inputData!=null ? inputData : inputArea.getText().getBytes(StandardCharsets.UTF_8))
                    : Base64.getDecoder().decode(inputArea.getText());

            if (doEncrypt) {
                List<BigInteger[]> ct = elgamal.encrypt(in);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                for (BigInteger[] pair: ct) {
                    byte[] aBytes = pair[0].toByteArray();
                    byte[] bBytes = pair[1].toByteArray();
                    bout.write((byte)aBytes.length);
                    bout.write(aBytes);
                    bout.write((byte)bBytes.length);
                    bout.write(bBytes);
                }
                outputData = bout.toByteArray();
                outputArea.setText(Base64.getEncoder().encodeToString(outputData));
            } else {
                byte[] all = Base64.getDecoder().decode(inputArea.getText());
                ByteArrayInputStream bin = new ByteArrayInputStream(all);
                List<BigInteger[]> ct = new java.util.ArrayList<>();
                while (bin.available()>0) {
                    int la = bin.read();
                    byte[] aBytes = bin.readNBytes(la);
                    int lb = bin.read();
                    byte[] bBytes = bin.readNBytes(lb);
                    ct.add(new BigInteger[]{ new BigInteger(aBytes), new BigInteger(bBytes) });
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