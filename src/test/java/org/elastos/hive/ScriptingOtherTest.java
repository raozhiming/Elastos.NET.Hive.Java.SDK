package org.elastos.hive;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ScriptingOtherTest {

	private String noConditionName = "get_groups";

	@Test
	public void test00_callOtherScript() {
		try {
			String ret = scripting.callScript(noConditionName, null, null, String.class).get();
			System.out.println("return=" + ret);
		} catch (Exception e) {
			fail();
		}
	}

	private static Scripting scripting;

	@BeforeClass
	public static void setUp() {
		Vault vault = UserFactory.createUser2().getVault();
		scripting = vault.getScripting();
	}
}
