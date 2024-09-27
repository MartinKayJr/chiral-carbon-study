package cn.martinkay.util;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import cn.martinkay.chiral.MdlMolParser;
import cn.martinkay.chiral.Molecule;

public class PubChemStealer {

    private static final String PUB_CHEM_SITE = "https://pubchem.ncbi.nlm.nih.gov";
    private static final String FAKE_PUB_CHEM_SITE = "https://ccc.zhenxin.me";//reserved proxy...

    @NotNull
    public static Molecule nextRandomMolecule() {
        Random r = new Random();
        for (int retry = 5; retry > 0; retry--) {
            long cid = (long) (r.nextDouble() * 100000000 + r.nextDouble() * 10000000 + r.nextDouble() * 100000 + 100000);
            try {
                return getMoleculeByCid(cid);
            } catch (IOException e) {
                e.printStackTrace();
                retry--;
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
        return null;
    }

    public static Molecule getMoleculeByCid(long cid) throws IOException, MdlMolParser.BadMolFormatException {
        HttpURLConnection conn = (HttpURLConnection) new URL(PUB_CHEM_SITE + "/rest/pug/compound/CID/" + cid + "/record/SDF/?record_type=2d&response_type=display").openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        if (conn.getResponseCode() != 200) {
            conn.disconnect();
            throw new IOException("Bad ResponseCode: " + conn.getResponseCode());
        }
        InputStream in = conn.getInputStream();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        in.close();
        conn.disconnect();
        String str = outStream.toString();
        return MdlMolParser.parseString(str);
    }
}
