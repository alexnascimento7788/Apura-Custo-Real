import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Locale;

public class App {
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final Font FONTE_PADRAO = new Font("Tahoma", Font.PLAIN, 26);
    private static final Font FONTE_NEGRITO = new Font("Tahoma", Font.BOLD, 26);
    private static final int[] LARGURAS = {160, 500, 200, 300, 600, 450, 250, 250, 200, 250, 250, 300, 320, 250, 250, 320, 250, 250, 250, 250, 250, 250, 250, 250, 320, 320};

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "2.5");
        configurarFonteGlobal(FONTE_PADRAO);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("TAKKEN - APURACAO DE CUSTO REAL");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setLayout(new BorderLayout());

            JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 20));
            JTextField txtBusca = new JTextField("12", 10);
            JButton btn = new JButton("BUSCAR DADOS");
            painelTopo.add(new JLabel("CÓDIGO:")); painelTopo.add(txtBusca); painelTopo.add(btn);
            frame.add(painelTopo, BorderLayout.NORTH);

            String[] colunas = {
                    "Empresa", "Razao", "Cod_Interno", "Cod_Barras", "Descricao_Takken",
                    "Material", "NCM", "CEST", "Estoque", "Custo_Projetado",
                    "Custo_Fiscal", "CUSTO_REAL", "Custo_Total_Real", "P_Venda_Varejo",
                    "P_Venda_Takken", "P_Venda_Total_Takken", "Lucro_Bruto", "ICMS",
                    "ICMS_ST", "IPI", "PIS", "COFINS", "IRPPJ", "CSLL", "Lucro_Liquido", "MARKDOWN"
            };

            DefaultTableModel modelo = new DefaultTableModel(colunas, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };

            JTable tabela = new JTable(modelo);
            tabela.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            tabela.setRowHeight(75);
            tabela.getTableHeader().setFont(FONTE_NEGRITO);

            configurarRenderizadores(tabela);
            aplicarLarguras(tabela);

            frame.add(new JScrollPane(tabela), BorderLayout.CENTER);

            btn.addActionListener(e -> {
                modelo.setRowCount(0);
                String filtro = txtBusca.getText().trim();
                String condicao = filtro.isEmpty() ? "1=1" : "cad.Codigo = " + filtro;

                String sql = "SELECT est.id_loja, emp.razao, cad.Codigo, lista_codbarra(cad.Codigo) as barras, cad.DESCRICAO, ncm.descricao as ncm_desc, exp.ncm, exp.cest, est.LOJAEST, cad.CUSMEDIO, cad.VENDA, sim.valor as venda_takken, custo.custoreal as custo_base, icms.icms_origem, icms.icms_destino, cad.ipi as aliq_ipi, cad.aliq_pis_sai, cad.aliq_cofins_sai, emp.irpj, emp.csll FROM cadmer cad LEFT JOIN cadmer_estoque est ON est.codigo = cad.Codigo JOIN sef emp ON emp.id = est.id_loja JOIN cadmer_exportacao exp ON exp.codigo = cad.Codigo AND exp.id_loja = est.id_loja JOIN tkn_ncm ncm ON ncm.ncm = exp.ncm JOIN tkn_precos_simulados sim ON sim.cod_interno = cad.Codigo LEFT JOIN (SELECT ent.CODIGO, ent.codbarra, min(ent.custoreal) as custoreal FROM entcab cab INNER JOIN entradas ent ON cab.Id = ent.id_entcab WHERE ent.custoreal > 0 AND cab.UF = 'SC' AND cab.CFOP not in (1906,1907,2152,6152) GROUP BY ent.CODIGO, ent.codbarra) custo ON (custo.CODIGO = cad.Codigo OR (custo.codbarra IS NOT NULL AND custo.codbarra = lista_codbarra(cad.Codigo))) LEFT JOIN (SELECT i.id_cadmer_icms_cab, i.icms_origem, i.icms_destino FROM cadmer_icms i WHERE i.uforigem = 'SC' AND i.ufdestino = 'MG') icms ON icms.id_cadmer_icms_cab = cad.id_cadmer_icms_cab WHERE " + condicao;

                new SwingWorker<Void, Object[]>() {
                    @Override protected Void doInBackground() throws Exception {
                        try (Connection con = ConexaoDB.getConexao(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                            while (rs.next()) {
                                Mercadoria m = new Mercadoria();
                                m.codInterno = rs.getInt("Codigo");
                                m.estoque = parseBra(rs.getString("LOJAEST"));
                                m.custoFiscal = parseBra(rs.getString("CUSMEDIO"));
                                m.custoRealBase = parseBra(rs.getString("custo_base"));
                                m.pVendaTakken = parseBra(rs.getString("venda_takken"));
                                m.aliqIcms = parseBra(rs.getString("icms_origem"));
                                m.aliqIpi = parseBra(rs.getString("aliq_ipi"));
                                m.aliqCofinsSai = parseBra(rs.getString("aliq_cofins_sai"));
                                m.irppj = parseBra(rs.getString("irpj"));
                                m.csll = parseBra(rs.getString("csll"));

                                double crFinal = m.getCustoRealFinal();
                                Object[] linha = {
                                        rs.getString(1), rs.getString(2), m.codInterno, rs.getString("barras"), rs.getString("DESCRICAO"),
                                        rs.getString("ncm_desc"), rs.getString("ncm"), rs.getString("cest"),
                                        m.estoque, "", m.custoFiscal,
                                        crFinal, (m.estoque * crFinal),
                                        parseBra(rs.getString("VENDA")), m.pVendaTakken, (m.estoque * m.pVendaTakken),
                                        (m.pVendaTakken - m.custoRealBase),
                                        m.getIcms(), parseBra(rs.getString("icms_destino")), m.getIpi(), m.getPis(), m.getCofins(), m.getIrppj(), m.getCsll(),
                                        m.getLucroLiquido(), m.getMarkdown()
                                };
                                publish(linha);
                            }
                        } catch (Exception ex) { ex.printStackTrace(); }
                        return null;
                    }
                    @Override protected void process(java.util.List<Object[]> chunks) { for (Object[] linha : chunks) { modelo.addRow(linha); } }
                }.execute();
            });
            frame.setVisible(true);
        });
    }

    private static double parseBra(String s) {
        if (s == null || s.trim().isEmpty()) return 0.0;
        try { return Double.parseDouble(s.replace(",", ".")); } catch (Exception e) { return 0.0; }
    }

    private static void aplicarLarguras(JTable t) {
        for (int i = 0; i < t.getColumnCount(); i++) {
            if (i < LARGURAS.length) t.getColumnModel().getColumn(i).setPreferredWidth(LARGURAS[i]);
        }
    }

    private static void configurarRenderizadores(JTable t) {
        // Renderizador Centralizado para Moeda
        DefaultTableCellRenderer m = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) {
                if (v instanceof Double) {
                    setHorizontalAlignment(0); // 0 = Center
                    v = MOEDA.format(v);
                }
                super.setValue(v);
            }
        };

        // Renderizador Centralizado para Estoque (Sem .0)
        DefaultTableCellRenderer e = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) {
                if (v instanceof Double) {
                    setHorizontalAlignment(0); // 0 = Center
                    double d = (Double) v;
                    v = (d == (long) d) ? String.format("%d", (long) d) : String.format("%.2f", d);
                }
                super.setValue(v);
            }
        };

        // Renderizador Centralizado para Markdown (%)
        DefaultTableCellRenderer perc = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) {
                if (v instanceof Double) {
                    setHorizontalAlignment(0); // 0 = Center
                    v = String.format("%.2f %%", v);
                }
                super.setValue(v);
            }
        };

        // Aplica centralização em todas as colunas de dados
        for (int i = 0; i < t.getColumnCount(); i++) {
            if (i == 8) {
                t.getColumnModel().getColumn(i).setCellRenderer(e);
            } else if (i == 25) {
                t.getColumnModel().getColumn(i).setCellRenderer(perc);
            } else if (i >= 9) {
                t.getColumnModel().getColumn(i).setCellRenderer(m);
            } else {
                DefaultTableCellRenderer c = new DefaultTableCellRenderer();
                c.setHorizontalAlignment(0);
                t.getColumnModel().getColumn(i).setCellRenderer(c);
            }
        }
    }

    private static void configurarFonteGlobal(Font f) { Enumeration<Object> k = UIManager.getDefaults().keys(); while (k.hasMoreElements()) { Object key = k.nextElement(); if (UIManager.get(key) instanceof javax.swing.plaf.FontUIResource) UIManager.put(key, f); } }
}