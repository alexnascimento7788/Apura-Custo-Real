public class Mercadoria {
    public int codInterno;
    public String descricao;
    public double estoque;
    public double custoFiscal;
    public double custoRealBase;
    public double pVendaTakken;
    public double aliqIcms;
    public double aliqIpi;
    public double aliqCofinsSai;
    public double irppj;
    public double csll;

    private double getAliqIcmsTraduzida() {
        return (aliqIcms == 0) ? 0 : aliqIcms / 10000.0;
    }

    private double getFatorIpiBanco() {
        return (aliqIpi == 0) ? 0 : aliqIpi / 100.0;
    }

    public double getIcms() {
        double fator = getAliqIcmsTraduzida();
        return (pVendaTakken * fator) - (custoFiscal * fator);
    }

    public double getIpi() {
        double fator = getFatorIpiBanco();
        double calculo = (custoFiscal * fator) - (pVendaTakken * fator);
        return Math.round(Math.abs(calculo) * 100.0) / 100.0;
    }

    public double getPis() {
        double fatorIcms = getAliqIcmsTraduzida();
        double baseCalculo = pVendaTakken - (pVendaTakken * fatorIcms);
        double resultado = baseCalculo * 0.0065;
        return Math.round(resultado * 100.0) / 100.0;
    }

    public double getCofins() {
        double fatorIcms = getAliqIcmsTraduzida();
        double baseCalculo = pVendaTakken - (pVendaTakken * fatorIcms);
        double resultado = baseCalculo * (aliqCofinsSai / 100.0);
        return Math.round(resultado * 100.0) / 100.0;
    }

    public double getIrppj() {
        return Math.round((pVendaTakken * (irppj / 100.0)) * 100.0) / 100.0;
    }

    public double getCsll() {
        return Math.round((pVendaTakken * (csll / 100.0)) * 100.0) / 100.0;
    }

    public double getCustoRealFinal() {
        double impostos = getIcms() + getIpi() + getPis() + getCofins() + getIrppj() + getCsll();
        return Math.round((custoRealBase + impostos) * 100.0) / 100.0;
    }

    public double getLucroLiquido() {
        return Math.round((pVendaTakken - getCustoRealFinal()) * 100.0) / 100.0;
    }

    public double getMarkdown() {
        double lucro = getLucroLiquido();
        if (lucro <= 0 || pVendaTakken == 0) return 0.0;
        double mdm = (lucro / pVendaTakken) * 100.0;
        return Math.round(mdm * 100.0) / 100.0;
    }
}