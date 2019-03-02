package diffson;

import java.io.File;
import java.util.Date;

import org.junit.Test;

import fr.inria.coming.main.ComingProperties;

/**
 * Experiment runners
 */
public class ExperimentRunner {

	@Test
	public void testICSE2015() throws Exception {
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer();
		analyzer.run(ComingProperties.getProperty("icse15difffolder"));
	}

	@Test
	public void testICSE15() throws Exception {
		ComingProperties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("MAX_AST_CHANGES_PER_FILE", "20");
		File outFile = new File("./out/ICSE2015_" + (new Date()));
		String out = outFile.getAbsolutePath();
		outFile.mkdirs();
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
		String input = new File("./datasets/icse2015").getAbsolutePath();
		ComingProperties.properties.setProperty("icse15difffolder", input);
		analyzer.run(ComingProperties.getProperty("icse15difffolder"));

	}

	@Test
	public void testHDRepair() throws Exception {
		ComingProperties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("MAX_AST_CHANGES_PER_FILE", "20");
		File outFile = new File("./out/HdRepair_" + (new Date()));
		String out = outFile.getAbsolutePath();
		outFile.mkdirs();
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
		String input = new File("/Users/matias/develop/sketch-repair/datasets/pairs-bug-fixes-saner16")
				.getAbsolutePath();
		ComingProperties.properties.setProperty("icse15difffolder", input);
		analyzer.run(ComingProperties.getProperty("icse15difffolder"));

	}

	@Test
	public void testICSE2018All() throws Exception {
		ComingProperties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("MAX_AST_CHANGES_PER_FILE", "20");
		File outFile = new File("./out/icse18_" + (new Date()));
		String out = outFile.getAbsolutePath();
		outFile.mkdirs();
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
		String input = new File("/Users/matias/develop/sketch-repair/git-sketch4repair/datasets/icse2018-pairs-all")
				.getAbsolutePath();
		ComingProperties.properties.setProperty("icse15difffolder", input);
		analyzer.run(ComingProperties.getProperty("icse15difffolder"));

	}

	public static void main(String[] args) throws Exception {
		// String name = args[0];
		String inputpath = args[0];
		String output = args[1];

		File outFile = new File(output);
		String out = outFile.getAbsolutePath();
		outFile.mkdirs();
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
		String input = new File(inputpath).getAbsolutePath();
		ComingProperties.properties.setProperty("icse15difffolder", input);
		analyzer.run(ComingProperties.getProperty("icse15difffolder"));
	}

	@Test
	public void testD4J() throws Exception {
		ComingProperties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("MAX_AST_CHANGES_PER_FILE", "200");
		File outFile = new File("./out/Defects4J_" + (new Date()));
		String out = outFile.getAbsolutePath();
		outFile.mkdirs();
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
		String input = new File("./datasets/Defects4J").getAbsolutePath();
		ComingProperties.properties.setProperty("icse15difffolder", input);
		analyzer.run(ComingProperties.getProperty("icse15difffolder"));
	}

	@Test
	public void testCODEREP() throws Exception {
		ComingProperties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("excludetests", "false");
		for (int i = 3; i <= 4; i++) {
			File outFile = new File("./out/" + "codeRepDS" + i + "_" + (new Date()));
			String out = outFile.getAbsolutePath();
			outFile.mkdirs();
			DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
			String input = new File(
					// "./datasets/codeRepDS" + i
					"/Users/matias/develop/sketch-repair/git-sketch4repair/datasets/CodeRep/ds_pairs/result_Dataset" + i
							+ "_unidiff").getAbsolutePath();
			ComingProperties.properties.setProperty("icse15difffolder", input);
			analyzer.run(ComingProperties.getProperty("icse15difffolder"));
		}
	}

	@Test
	public void testD4Reload() throws Exception {
		ComingProperties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("MAX_AST_CHANGES_PER_FILE", "200");
		File outFile = new File("./out/Defects4JReload_" + (new Date()));
		String out = outFile.getAbsolutePath();
		outFile.mkdirs();
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
		String input = new File("/Users/matias/develop/defects4-repair-reloaded/pairs/D_unassessed/").getAbsolutePath();
		ComingProperties.properties.setProperty("icse15difffolder", input);
		analyzer.run(ComingProperties.getProperty("icse15difffolder"));
	}

	@Test
	public void test3Sfix() throws Exception {
		ComingProperties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("max_synthesis_step", "100000");
		ComingProperties.properties.setProperty("MAX_AST_CHANGES_PER_FILE", "200");
		File outFile = new File("./out/3fixtest_" + (new Date()));
		String out = outFile.getAbsolutePath();
		outFile.mkdirs();
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
		String input = new File("/Users/matias/develop/overfitting/overfitting-data/data/rowdata/3sFix_files_pair/")
				.getAbsolutePath();
		ComingProperties.properties.setProperty("icse15difffolder", input);
		analyzer.run(ComingProperties.getProperty("icse15difffolder"));
	}

}
