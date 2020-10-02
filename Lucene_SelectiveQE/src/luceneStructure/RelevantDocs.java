package luceneStructure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

//Class that contains a list of selected relevant documents for each topic
public class RelevantDocs {

	public static String [][] RELEVANT_DOCS = new String [][] {
		{"3b5jzndg.txt","3b5jzndg.txt", "jmrg4oeb.txt", "jmrg4oeb.txt", "87d7gzgb.txt"},
		{"147yc66p.txt","1x3dp1vv.txt", "t7c0drft.txt", "a4282jtk.txt", "lmjaldcs.txt"},
		{"bc3108hi.txt","iy4knx7j.txt", "jfe8neec.txt", "ozvdz9ew.txt", "pho6dksc.txt"},
		{"0lwmzjxz.txt","0nhgxoim.txt", "13akn7dm.txt", "4giwnrbw.txt", "5gbjdr0u.txt"},
		{"651uz4ev.txt","6dqy7tzc.txt", "679qfp2s.txt", "p6uliadr.txt", "uhwg608l.txt"},
		{"4mj7n0ax.txt","5ofx84mw.txt", "nkp80h8b.txt", "xl2u8asv.txt", "pvlg3nkp.txt"},
		{"0te5ybjv.txt","15rcq4sh.txt", "ok3h4w7f.txt", "tgowzrqo.txt", "8kezr83s.txt"},
		{"4kkzi70v.txt","8rkwmxor.txt", "k4nf0c6n.txt", "t4d2ibng.txt", "lysrwv2f.txt"},
		{"4dnzjeyp.txt","51g3vhcx.txt", "8wlxkflg.txt", "a7u8xdux.txt", "aj1cod3x.txt"},
		{"6l25y6fw.txt","6okpsuvu.txt", "aiq6ejcq.txt", "q5xc4m3j.txt", "qjf23a7e.txt"},
		{"6cr9h7no.txt"},
		{"oljg1ldn.txt","qznrjflx.txt", "w8dfhxyt.txt", "r79doruj.txt", "rtybezro.txt"},
		{"00rq0ggi.txt","n404p6tq.txt", "a47onmje.txt", "bbghqy1a.txt", "vw2d7spi.txt"},
		{"05w8tv8x.txt","58tj4csz.txt", "ibwo59yh.txt", "gs2wi7hr.txt", "zz4cczuj.txt"},
		{"90qq0xsw.txt","3q3sktuq.txt", "80ev0j5a.txt", "89n52jny.txt", "90qq0xsw.txt"},
		{"38rov4ux.txt","3q3sktuq.txt", "40ktyt5q.txt", "61ta81iy.txt", "75c4wvhq.txt"},
		{"0b0fvnhn.txt","0pigqtzt.txt", "14he8n3u.txt", "n0mz098o.txt", "z2y1ywdq.txt"},
		{"0dwlaafj.txt","1l89qtfd.txt", "3khv6yrc.txt", "ic2h01gm.txt", "hm516x6p.txt"},
		{"4s57ls6y.txt","c69vfs8q.txt", "k8k5kg4z.txt", "n9wox9lg.txt", "vpodtbjk.txt"},
		{"118x15od.txt","16fzqskz.txt", "53qxs7ge.txt", "eqk8a34e.txt", "xwaycskl.txt"},
		{"dcf6bl8f.txt","di7hfghi.txt", "ll9x8okq.txt", "huyl21vz.txt", "pn516wom.txt"},
		{"0kctqmbu.txt","3afy5ent.txt", "4nbk0lmp.txt", "5fcixqol.txt", "i7j15rxy.txt"},
		{"0euaaspo.txt","15rpskir.txt", "60wcvkbn.txt", "lujxql3a.txt", "k65501xp.txt"},
		{"118x15od.txt","3zhstfss.txt", "4cx6fe5v.txt", "4ro7x2ce.txt", "o3in3rdo.txt"},
		{"92jsajsu.txt","9ynljm2r.txt", "as6tbfrh.txt", "mn3b6nrs.txt", "ny2uqeor.txt"},
		{"j8odxs1h.txt","jahm572k.txt", "jsyao6qu.txt", "uwj62cuv.txt", "vgkiadky.txt"},
		{"54260tth.txt","azdu4rgp.txt", "bqq88zib.txt", "wwr777rk.txt", "zbzrxuoh.txt"},
		{"29kjytsn.txt","2rhhw3sx.txt", "31iws3lh.txt", "aku5atqh.txt", "ehidj2ev.txt"},
		{"4fntk91q.txt","69toerzi.txt", "9hvojcnk.txt", "6g34qwer.txt", "n0mz098o.txt"},
		{"8xz0ddci.txt","8znnq0rh.txt", "bzeqs5oh.txt", "j0i9ozsz.txt", "n0mz098o.txt"},
		{"39mfts0g.txt","3p2dl8yf.txt", "431ksdno.txt", "ccxr0s5c.txt", "g6m3k7vd.txt"},
		{"i758v1vb.txt","j99cgsjt.txt", "jnkszwea.txt", "suhqgmlo.txt", "wg86ws3b.txt"},
		{"0yqyclxk.txt","39mu0tdr.txt", "54mx8v4i.txt", "8gncbgot.txt", "gu2mt6zp.txt"},
		{"4989atst.txt","91vbs5ap.txt", "bzc7luwj.txt", "fa7fa10r.txt", "tb6uxn7n.txt"},
		{"2uwnamao.txt","33m59ajn.txt", "4n6v5kfv.txt", "6xkm2j0f.txt", "i1g9ikdb.txt"}
	};

	//--------------------------------------------------
	//Method performing an expanded query and write its results in an output file
	//--------------------------------------------------
	public static void main (String [] args) {
		System.out.println(RelevantDocs.RELEVANT_DOCS[1][0]);
	}

}
