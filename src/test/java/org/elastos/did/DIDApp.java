package org.elastos.did;

import org.elastos.did.adapter.DummyAdapter;
import org.elastos.did.exception.DIDException;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class DIDApp  extends Entity {
	private Issuer issuer;

	public DIDApp(String name, String mnemonic, DummyAdapter adapter,  String phrasepass, String storepass) throws DIDException {
		super(name, mnemonic, adapter, phrasepass, storepass);
		issuer = new Issuer(getDocument());
	}

	public VerifiableCredential issueDiplomaFor(DApp dapp) throws DIDException {
		Map<String, String> subject = new HashMap<String, String>();
		subject.put("appDid", dapp.appId);

		Calendar exp = Calendar.getInstance();
		exp.add(Calendar.YEAR, 5);

		Issuer.CredentialBuilder cb = issuer.issueFor(dapp.getDid());
		VerifiableCredential vc = cb.id("didapp")
				.type("AppIdCredential")
				.properties(subject)
				.expirationDate(exp.getTime())
				.seal(getStorePassword());

		System.out.println("VerifiableCredential:");
		String vcStr = vc.toJson(true, true);
		System.out.println(vcStr);

		return vc;
	}
}
