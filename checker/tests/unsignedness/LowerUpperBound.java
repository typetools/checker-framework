import org.checkerframework.checker.unsignedness.qual.*;

@skip-test

public class LowerUpperBound {

	void good() {
		@Unsigned int unsigned = 1;
		@Signed int signed = 1;
		@Constant int constant = 1;
		@UnknownSignedness int unknown = 1;

		// Confirm that * follows the LUB rule for legal uses.
		@UnknownSignedness int unMultUnUn = unknown * unknown;
		@UnknownSignedness int unMultUnU = unknown * unsigned;
		@UnknownSignedness int unMultUnS = unknown * signed;
		@UnknownSignedness int unMultUnC = unknown * constant;

		@UnknownSignedness int unMultUUn = unsigned * unknown;
		@UnknownSignedness int unMultUU = unsigned * unsigned;
		@Unsigned int uMultUU = unsigned * unsigned;
		@UnknownSignedness int unMultUC = unsigned * constant;
		@Unsigned int uMultUC = unsigned * constant;

		@UnknownSignedness int unMultSUn = signed * unknown;
		@UnknownSignedness int unMultSS = signed * signed;
		@Signed int sMultSS = signed * signed;
		@UnknownSignedness int unMultSC = signed * constant;
		@Signed int sMultSC = signed * constant;

		@UnknownSignedness int unMultCUn = constant * unknown;
		@UnknownSignedness int unMultCU = constant * unsigned;
		@Unsigned int uMultCU = constant * unsigned;
		@UnknownSignedness int unMultCS = constant * signed;
		@Signed int sMultCS = constant * signed;
		@UnknownSignedness int unMultCC = constant * constant;
		@Unsigned int uMultCC = constant * constant;
		@Signed int sMultCC = constant * constant;
		@Constant int cMultCC = constant * constant;

		// Confirm that / follows the LUB rule for legal uses.
		@UnknownSignedness int unDivUnUn = unknown / unknown;
		@UnknownSignedness int unDiveUnS = unknown / signed;
		@UnknownSignedness int unDivUnC = unknown / constant;

		@UnknownSignedness int unDivSUn = signed / unknown;
		@UnknownSignedness int unDivSS = signed / signed;
		@Signed int sDivSS = signed / signed;
		@UnknownSignedness int unDivSC = signed / constant;
		@Signed int sDivSC = signed / constant;

		@UnknownSignedness int unDivCUn = constant / unknown;
		@UnknownSignedness int unDivCS = constant / signed;
		@Signed int sDivCS = constant / signed;
		@UnknownSignedness int unDivCC = constant / constant;
		@Unsigned int uDivCC = constant / constant;
		@Signed int sDivCC = constant / constant;
		@Constant int cDivCC = constant / constant;

		// Confirm that % follows the LUB ruel for legal uses.
		@UnknownSignedness int unModUnUn = unknown % unknown;
		@UnknownSignedness int unModeUnS = unknown % signed;
		@UnknownSignedness int unModUnC = unknown % constant;

		@UnknownSignedness int unModSUn = signed % unknown;
		@UnknownSignedness int unModSS = signed % signed;
		@Signed int sModSS = signed % signed;
		@UnknownSignedness int unModSC = signed % constant;
		@Signed int sModSC = signed % constant;

		@UnknownSignedness int unModCUn = constant % unknown;
		@UnknownSignedness int unModCS = constant % signed;
		@Signed int sModCS = constant % signed;
		@UnknownSignedness int unModCC = constant % constant;
		@Unsigned int uModCC = constant % constant;
		@Signed int sModCC = constant % constant;
		@Constant int cModCC = constant % constant;

		// Confirm that + follows the LUB rule for legal uses.
		@UnknownSignedness int unAddUnUn = unknown + unknown;
		@UnknownSignedness int unAddUnU = unknown + unsigned;
		@UnknownSignedness int unAddUnS = unknown + signed;
		@UnknownSignedness int unAddUnC = unknown + constant;

		@UnknownSignedness int unAddUUn = unsigned + unknown;
		@UnknownSignedness int unAddUU = unsigned + unsigned;
		@Unsigned int uAddUU = unsigned + unsigned;
		@UnknownSignedness int unAddUC = unsigned + constant;
		@Unsigned int uAddUC = unsigned + constant;

		@UnknownSignedness int unAddSUn = signed + unknown;
		@UnknownSignedness int unAddSS = signed + signed;
		@Signed int sAddSS = signed + signed;
		@UnknownSignedness int unAddSC = signed + constant;
		@Signed int sAddSC = signed + constant;

		@UnknownSignedness int unAddCUn = constant + unknown;
		@UnknownSignedness int unAddCU = constant + unsigned;
		@Unsigned int uAddCU = constant + unsigned;
		@UnknownSignedness int unAddCS = constant + signed;
		@Signed int sAddCS = constant + signed;
		@UnknownSignedness int unAddCC = constant + constant;
		@Unsigned int uAddCC = constant + constant;
		@Signed int sAddCC = constant + constant;
		@Constant int cAddCC = constant + constant;

		// Confirm that - follows the LUB rule for legal uses.
		@UnknownSignedness int unSubUnUn = unknown - unknown;
		@UnknownSignedness int unSubUnU = unknown - unsigned;
		@UnknownSignedness int unSubUnS = unknown - signed;
		@UnknownSignedness int unSubUnC = unknown - constant;

		@UnknownSignedness int unSubUUn = unsigned - unknown;
		@UnknownSignedness int unSubUU = unsigned - unsigned;
		@Unsigned int uSubUU = unsigned - unsigned;
		@UnknownSignedness int unSubUC = unsigned - constant;
		@Unsigned int uSubUC = unsigned - constant;

		@UnknownSignedness int unSubSUn = signed - unknown;
		@UnknownSignedness int unSubSS = signed - signed;
		@Signed int sSubSS = signed - signed;
		@UnknownSignedness int unSubSC = signed - constant;
		@Signed int sSubSC = signed - constant;

		@UnknownSignedness int unSubCUn = constant - unknown;
		@UnknownSignedness int unSubCU = constant - unsigned;
		@Unsigned int uSubCU = constant - unsigned;
		@UnknownSignedness int unSubCS = constant - signed;
		@Signed int sSubCS = constant - signed;
		@UnknownSignedness int unSubCC = constant - constant;
		@Unsigned int uSubCC = constant - constant;
		@Signed int sSubCC = constant - constant;
		@Constant int cSubCC = constant - constant;

		// Confirm that << follows the LUB rule for legal uses.
		@UnknownSignedness int unLShiftUnUn = unknown << unknown;
		@UnknownSignedness int unLShiftUnU = unknown << unsigned;
		@UnknownSignedness int unLShiftUnS = unknown << signed;
		@UnknownSignedness int unLShiftUnC = unknown << constant;

		@UnknownSignedness int unLShiftUUn = unsigned << unknown;
		@UnknownSignedness int unLShiftUU = unsigned << unsigned;
		@Unsigned int uLShiftUU = unsigned << unsigned;
		@UnknownSignedness int unLShiftUC = unsigned << constant;
		@Unsigned int uLShiftUC = unsigned << constant;

		@UnknownSignedness int unLShiftSUn = signed << unknown;
		@UnknownSignedness int unLShiftSS = signed << signed;
		@Signed int sLShiftSS = signed << signed;
		@UnknownSignedness int unLShiftSC = signed << constant;
		@Signed int sLShiftSC = signed << constant;

		@UnknownSignedness int unLShiftCUn = constant << unknown;
		@UnknownSignedness int unLShiftCU = constant << unsigned;
		@Unsigned int uLShiftCU = constant << unsigned;
		@UnknownSignedness int unLShiftCS = constant << signed;
		@Signed int sLShiftCS = constant << signed;
		@UnknownSignedness int unLShiftCC = constant << constant;
		@Unsigned int uLShiftCC = constant << constant;
		@Signed int sLShiftCC = constant << constant;
		@Constant int cLShiftCC = constant << constant;

		// Confirm that >> follows the LUB rule for legal uses.
		@UnknownSignedness int unSShiftUnUn = unknown >> unknown;
		@UnknownSignedness int unSShiftUnU = unknown >> unsigned;
		@UnknownSignedness int unSShiftUnS = unknown >> signed;
		@UnknownSignedness int unSShiftUnC = unknown >> constant;

		@UnknownSignedness int unSShiftSUn = signed >> unknown;
		@UnknownSignedness int unSShiftSS = signed >> signed;
		@Signed int sSShiftSS = signed >> signed;
		@UnknownSignedness int unSShiftSC = signed >> constant;
		@Signed int sSShiftSC = signed >> constant;

		@UnknownSignedness int unSShiftCUn = constant >> unknown;
		@UnknownSignedness int unSShiftCU = constant >> unsigned;
		@Unsigned int uSShiftCU = constant >> unsigned;
		@UnknownSignedness int unSShiftCS = constant >> signed;
		@Signed int sSShiftCS = constant >> signed;
		@UnknownSignedness int unSShiftCC = constant >> constant;
		@Unsigned int uSShiftCC = constant >> constant;
		@Signed int sSShiftCC = constant >> constant;
		@Constant int cSShiftCC = constant >> constant;

		// Confirm that >>> follows the LUB rule for legal uses.
		@UnknownSignedness int unUnShiftUnUn = unknown >>> unknown;
		@UnknownSignedness int unUnShiftUnU = unknown >>> unsigned;
		@UnknownSignedness int unUnShiftUnS = unknown >>> signed;
		@UnknownSignedness int unUnShiftUnC = unknown >>> constant;

		@UnknownSignedness int unUnShiftUUn = unsigned >>> unknown;
		@UnknownSignedness int unUnShiftUU = unsigned >>> unsigned;
		@Unsigned int uUnShiftUU = unsigned >>> unsigned;
		@UnknownSignedness int unUnShiftUC = unsigned >>> constant;
		@Unsigned int uUnShiftUC = unsigned >>> constant;

		@UnknownSignedness int unUnShiftCUn = constant >>> unknown;
		@UnknownSignedness int unUnShiftCU = constant >>> unsigned;
		@Unsigned int uUnShiftCU = constant >>> unsigned;
		@UnknownSignedness int unUnShiftCS = constant >>> signed;
		@Signed int sUnShiftCS = constant >>> signed;
		@UnknownSignedness int unUnShiftCC = constant >>> constant;
		@Unsigned int uUnShiftCC = constant >>> constant;
		@Signed int sUnShiftCC = constant >>> constant;
		@Constant int cUnShiftCC = constant >>> constant;

		// Confirm that & follows the LUB rule for legal uses.
		@UnknownSignedness int unANDUnUn = unknown & unknown;
		@UnknownSignedness int unANDUnU = unknown & unsigned;
		@UnknownSignedness int unANDUnS = unknown & signed;
		@UnknownSignedness int unANDUnC = unknown & constant;

		@UnknownSignedness int unANDUUn = unsigned & unknown;
		@UnknownSignedness int unANDUU = unsigned & unsigned;
		@Unsigned int uANDUU = unsigned & unsigned;
		@UnknownSignedness int unANDUC = unsigned & constant;
		@Unsigned int uANDUC = unsigned & constant;

		@UnknownSignedness int unANDSUn = signed & unknown;
		@UnknownSignedness int unANDSS = signed & signed;
		@Signed int sANDSS = signed & signed;
		@UnknownSignedness int unANDSC = signed & constant;
		@Signed int sANDSC = signed & constant;

		@UnknownSignedness int unANDCUn = constant & unknown;
		@UnknownSignedness int unANDCU = constant & unsigned;
		@Unsigned int uANDCU = constant & unsigned;
		@UnknownSignedness int unANDCS = constant & signed;
		@Signed int sANDCS = constant & signed;
		@UnknownSignedness int unANDCC = constant & constant;
		@Unsigned int uANDCC = constant & constant;
		@Signed int sANDCC = constant & constant;
		@Constant int cANDCC = constant & constant;

		// Confirm that ^ follows the LUB rule for legal uses.
		@UnknownSignedness int unXORUnUn = unknown ^ unknown;
		@UnknownSignedness int unXORUnU = unknown ^ unsigned;
		@UnknownSignedness int unXORUnS = unknown ^ signed;
		@UnknownSignedness int unXORUnC = unknown ^ constant;

		@UnknownSignedness int unXORUUn = unsigned ^ unknown;
		@UnknownSignedness int unXORUU = unsigned ^ unsigned;
		@Unsigned int uXORUU = unsigned ^ unsigned;
		@UnknownSignedness int unXORUC = unsigned ^ constant;
		@Unsigned int uXORUC = unsigned ^ constant;

		@UnknownSignedness int unXORSUn = signed ^ unknown;
		@UnknownSignedness int unXORSS = signed ^ signed;
		@Signed int sXORSS = signed ^ signed;
		@UnknownSignedness int unXORSC = signed ^ constant;
		@Signed int sXORSC = signed ^ constant;

		@UnknownSignedness int unXORCUn = constant ^ unknown;
		@UnknownSignedness int unXORCU = constant ^ unsigned;
		@Unsigned int uXORCU = constant ^ unsigned;
		@UnknownSignedness int unXORCS = constant ^ signed;
		@Signed int sXORCS = constant ^ signed;
		@UnknownSignedness int unXORCC = constant ^ constant;
		@Unsigned int uXORCC = constant ^ constant;
		@Signed int sXORCC = constant ^ constant;
		@Constant int cXORCC = constant ^ constant;

		// Confirm that | follows the LUB rule for legal uses.
		@UnknownSignedness int unORUnUn = unknown | unknown;
		@UnknownSignedness int unORUnU = unknown | unsigned;
		@UnknownSignedness int unORUnS = unknown | signed;
		@UnknownSignedness int unORUnC = unknown | constant;

		@UnknownSignedness int unORUUn = unsigned | unknown;
		@UnknownSignedness int unORUU = unsigned | unsigned;
		@Unsigned int uORUU = unsigned | unsigned;
		@UnknownSignedness int unORUC = unsigned | constant;
		@Unsigned int uORUC = unsigned | constant;

		@UnknownSignedness int unORSUn = signed | unknown;
		@UnknownSignedness int unORSS = signed | signed;
		@Signed int sORSS = signed | signed;
		@UnknownSignedness int unORSC = signed | constant;
		@Signed int sORSC = signed | constant;

		@UnknownSignedness int unORCUn = constant | unknown;
		@UnknownSignedness int unORCU = constant | unsigned;
		@Unsigned int uORCU = constant | unsigned;
		@UnknownSignedness int unORCS = constant | signed;
		@Signed int sORCS = constant | signed;
		@UnknownSignedness int unORCC = constant | constant;
		@Unsigned int uORCC = constant | constant;
		@Signed int sORCC = constant | constant;
		@Constant int cORCC = constant | constant;
	}

	void bad() {
		@UnknownSignedness int unknown = 1;
		@Unsigned int unsigned = 1;
		@Signed int signed = 1;
		@Constant int constant = 1;

		// Confirm that * follows the LUB rule for legal uses.
		//:: error: (assignment.type.incompatible)
		@Unsigned int uMultUnUn = unknown * unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sMultUnUn = unknown * unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultUnUn = unknown * unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultUnUn = unknown * unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uMultUnU = unknown * unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sMultUnU = unknown * unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultUnU = unknown * unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultUnU = unknown * unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uMultUnS = unknown * signed;
		//:: error: (assignment.type.incompatible)
		@Signed int sMultUnS = unknown * signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultUnS = unknown * signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultUnS = unknown * signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uMultUnC = unknown * constant;
		//:: error: (assignment.type.incompatible)
		@Signed int sMultUnC = unknown * constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultUnC = unknown * constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultUnC = unknown * constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uMultUUn = unsigned * unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sMultUUn = unsigned * unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultUUn = unsigned * unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultUUn = unsigned * unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sMultUU = unsigned * unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultUU = unsigned * unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultUU = unsigned * unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sMultUC = unsigned * constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultUC = unsigned * constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultUC = unsigned * constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uMultSUn = signed * unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sMultSUn = signed * unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultSUn = signed * unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultSUn = signed * unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uMultSS = signed * signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultSS = signed * signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultSS = signed * signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uMultSC = signed * constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultSC = signed * constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultSC = signed * constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uMultCUn = constant * unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sMultCUn = constant * unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultCUn = constant * unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultCUn = constant * unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sMultCU = constant * unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultCU = constant * unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultCU = constant * unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uMultCS = constant * signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cMultCS = constant * signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultCS = constant * signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubMultCC = constant * constant;


		// Confirm that / follows the LUB rule for legal uses.
		//:: error: (assignment.type.incompatible)
		@Unsigned int uDivUnUn = unknown / unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sDivUnUn = unknown / unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cDivUnUn = unknown / unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubDivUnUn = unknown / unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uDivUnS = unknown / signed;
		//:: error: (assignment.type.incompatible)
		@Signed int sDivUnS = unknown / signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cDivUnS = unknown / signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubDivUnS = unknown / signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uDivUnC = unknown / constant;
		//:: error: (assignment.type.incompatible)
		@Signed int sDivUnC = unknown / constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cDivUnC = unknown / constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubDivUnC = unknown / constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uDivSUn = signed / unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sDivSUn = signed / unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cDivSUn = signed / unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubDivSUn = signed / unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uDivSS = signed / signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cDivSS = signed / signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubDivSS = signed / signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uDivSC = signed / constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cDivSC = signed / constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubDivSC = signed / constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uDivCUn = constant / unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sDivCUn = constant / unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cDivCUn = constant / unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubDivCUn = constant / unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uDivCS = constant / signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cDivCS = constant / signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubDivCS = constant / signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubDivCC = constant / constant;


		// Confirm that % follows the LUB rule for legal uses.
		//:: error: (assignment.type.incompatible)
		@Unsigned int uModUnUn = unknown % unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sModUnUn = unknown % unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cModUnUn = unknown % unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubModUnUn = unknown % unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uModUnS = unknown % signed;
		//:: error: (assignment.type.incompatible)
		@Signed int sModUnS = unknown % signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cModUnS = unknown % signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubModUnS = unknown % signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uModUnC = unknown % constant;
		//:: error: (assignment.type.incompatible)
		@Signed int sModUnC = unknown % constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cModUnC = unknown % constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubModUnC = unknown % constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uModSUn = signed % unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sModSUn = signed % unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cModSUn = signed % unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubModSUn = signed % unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uModSS = signed % signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cModSS = signed % signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubModSS = signed % signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uModSC = signed % constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cModSC = signed % constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubModSC = signed % constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uModCUn = constant % unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sModCUn = constant % unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cModCUn = constant % unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubModCUn = constant % unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uModCS = constant % signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cModCS = constant % signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubModCS = constant % signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubModCC = constant % constant;


		// Confirm that + follows the LUB rule for legal uses.
		//:: error: (assignment.type.incompatible)
		@Unsigned int uAddUnUn = unknown + unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sAddUnUn = unknown + unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddUnUn = unknown + unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddUnUn = unknown + unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uAddUnU = unknown + unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sAddUnU = unknown + unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddUnU = unknown + unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddUnU = unknown + unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uAddUnS = unknown + signed;
		//:: error: (assignment.type.incompatible)
		@Signed int sAddUnS = unknown + signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddUnS = unknown + signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddUnS = unknown + signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uAddUnC = unknown + constant;
		//:: error: (assignment.type.incompatible)
		@Signed int sAddUnC = unknown + constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddUnC = unknown + constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddUnC = unknown + constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uAddUUn = unsigned + unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sAddUUn = unsigned + unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddUUn = unsigned + unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddUUn = unsigned + unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sAddUU = unsigned + unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddUU = unsigned + unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddUU = unsigned + unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sAddUC = unsigned + constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddUC = unsigned + constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddUC = unsigned + constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uAddSUn = signed + unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sAddSUn = signed + unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddSUn = signed + unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddSUn = signed + unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uAddSS = signed + signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddSS = signed + signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddSS = signed + signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uAddSC = signed + constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddSC = signed + constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddSC = signed + constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uAddCUn = constant + unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sAddCUn = constant + unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddCUn = constant + unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddCUn = constant + unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sAddCU = constant + unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddCU = constant + unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddCU = constant + unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uAddCS = constant + signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cAddCS = constant + signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddCS = constant + signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubAddCC = constant + constant;


		// Confirm that - follows the LUB rule for legal uses.
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSubUnUn = unknown - unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sSubUnUn = unknown - unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubUnUn = unknown - unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubUnUn = unknown - unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSubUnU = unknown - unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sSubUnU = unknown - unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubUnU = unknown - unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubUnU = unknown - unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSubUnS = unknown - signed;
		//:: error: (assignment.type.incompatible)
		@Signed int sSubUnS = unknown - signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubUnS = unknown - signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubUnS = unknown - signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSubUnC = unknown - constant;
		//:: error: (assignment.type.incompatible)
		@Signed int sSubUnC = unknown - constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubUnC = unknown - constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubUnC = unknown - constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uSubUUn = unsigned - unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sSubUUn = unsigned - unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubUUn = unsigned - unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubUUn = unsigned - unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sSubUU = unsigned - unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubUU = unsigned - unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubUU = unsigned - unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sSubUC = unsigned - constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubUC = unsigned - constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubUC = unsigned - constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uSubSUn = signed - unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sSubSUn = signed - unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubSUn = signed - unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubSUn = signed - unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSubSS = signed - signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubSS = signed - signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubSS = signed - signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSubSC = signed - constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubSC = signed - constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubSC = signed - constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uSubCUn = constant - unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sSubCUn = constant - unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubCUn = constant - unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubCUn = constant - unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sSubCU = constant - unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubCU = constant - unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubCU = constant - unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSubCS = constant - signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cSubCS = constant - signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubCS = constant - signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSubCC = constant - constant;


		// Confirm that << follows the LUB rule for legal uses.
		//:: error: (assignment.type.incompatible)
		@Unsigned int uLShiftUnUn = unknown << unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sLShiftUnUn = unknown << unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftUnUn = unknown << unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftUnUn = unknown << unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uLShiftUnU = unknown << unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sLShiftUnU = unknown << unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftUnU = unknown << unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftUnU = unknown << unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uLShiftUnS = unknown << signed;
		//:: error: (assignment.type.incompatible)
		@Signed int sLShiftUnS = unknown << signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftUnS = unknown << signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftUnS = unknown << signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uLShiftUnC = unknown << constant;
		//:: error: (assignment.type.incompatible)
		@Signed int sLShiftUnC = unknown << constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftUnC = unknown << constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftUnC = unknown << constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uLShiftUUn = unsigned << unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sLShiftUUn = unsigned << unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftUUn = unsigned << unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftUUn = unsigned << unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sLShiftUU = unsigned << unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftUU = unsigned << unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftUU = unsigned << unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sLShiftUC = unsigned << constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftUC = unsigned << constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftUC = unsigned << constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uLShiftSUn = signed << unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sLShiftSUn = signed << unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftSUn = signed << unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftSUn = signed << unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uLShiftSS = signed << signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftSS = signed << signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftSS = signed << signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uLShiftSC = signed << constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftSC = signed << constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftSC = signed << constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uLShiftCUn = constant << unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sLShiftCUn = constant << unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftCUn = constant << unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftCUn = constant << unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sLShiftCU = constant << unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftCU = constant << unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftCU = constant << unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uLShiftCS = constant << signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cLShiftCS = constant << signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftCS = constant << signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubLShiftCC = constant << constant;


		// Confirm that >> follows the LUB rule for legal uses.
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSShiftUnUn = unknown >> unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sSShiftUnUn = unknown >> unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cSShiftUnUn = unknown >> unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSShiftUnUn = unknown >> unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSShiftUnU = unknown >> unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sSShiftUnU = unknown >> unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cSShiftUnU = unknown >> unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSShiftUnU = unknown >> unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSShiftUnS = unknown >> signed;
		//:: error: (assignment.type.incompatible)
		@Signed int sSShiftUnS = unknown >> signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cSShiftUnS = unknown >> signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSShiftUnS = unknown >> signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSShiftUnC = unknown >> constant;
		//:: error: (assignment.type.incompatible)
		@Signed int sSShiftUnC = unknown >> constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cSShiftUnC = unknown >> constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSShiftUnC = unknown >> constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uSShiftSUn = signed >> unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sSShiftSUn = signed >> unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cSShiftSUn = signed >> unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSShiftSUn = signed >> unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSShiftSS = signed >> signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cSShiftSS = signed >> signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSShiftSS = signed >> signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSShiftSC = signed >> constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cSShiftSC = signed >> constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSShiftSC = signed >> constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uSShiftCUn = constant >> unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sSShiftCUn = constant >> unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cSShiftCUn = constant >> unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSShiftCUn = constant >> unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sSShiftCU = constant >> unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cSShiftCU = constant >> unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSShiftCU = constant >> unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uSShiftCS = constant >> signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cSShiftCS = constant >> signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSShiftCS = constant >> signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubSShiftCC = constant >> constant;


		// Confirm that >>> follows the LUB rule for legal uses.
		//:: error: (assignment.type.incompatible)
		@Unsigned int uUnShiftUnUn = unknown >>> unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sUnShiftUnUn = unknown >>> unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cUnShiftUnUn = unknown >>> unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubUnShiftUnUn = unknown >>> unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uUnShiftUnU = unknown >>> unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sUnShiftUnU = unknown >>> unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cUnShiftUnU = unknown >>> unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubUnShiftUnU = unknown >>> unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uUnShiftUnS = unknown >>> signed;
		//:: error: (assignment.type.incompatible)
		@Signed int sUnShiftUnS = unknown >>> signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cUnShiftUnS = unknown >>> signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubUnShiftUnS = unknown >>> signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uUnShiftUnC = unknown >>> constant;
		//:: error: (assignment.type.incompatible)
		@Signed int sUnShiftUnC = unknown >>> constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cUnShiftUnC = unknown >>> constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubUnShiftUnC = unknown >>> constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uUnShiftUUn = unsigned >>> unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sUnShiftUUn = unsigned >>> unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cUnShiftUUn = unsigned >>> unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubUnShiftUUn = unsigned >>> unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sUnShiftUU = unsigned >>> unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cUnShiftUU = unsigned >>> unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubUnShiftUU = unsigned >>> unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sUnShiftUC = unsigned >>> constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cUnShiftUC = unsigned >>> constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubUnShiftUC = unsigned >>> constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uUnShiftCUn = constant >>> unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sUnShiftCUn = constant >>> unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cUnShiftCUn = constant >>> unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubUnShiftCUn = constant >>> unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sUnShiftCU = constant >>> unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cUnShiftCU = constant >>> unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubUnShiftCU = constant >>> unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uUnShiftCS = constant >>> signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cUnShiftCS = constant >>> signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubUnShiftCS = constant >>> signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubUnShiftCC = constant >>> constant;


		// Confirm that & follows the LUB rule for legal uses.
		//:: error: (assignment.type.incompatible)
		@Unsigned int uANDUnUn = unknown & unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sANDUnUn = unknown & unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDUnUn = unknown & unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDUnUn = unknown & unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uANDUnU = unknown & unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sANDUnU = unknown & unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDUnU = unknown & unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDUnU = unknown & unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uANDUnS = unknown & signed;
		//:: error: (assignment.type.incompatible)
		@Signed int sANDUnS = unknown & signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDUnS = unknown & signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDUnS = unknown & signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uANDUnC = unknown & constant;
		//:: error: (assignment.type.incompatible)
		@Signed int sANDUnC = unknown & constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDUnC = unknown & constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDUnC = unknown & constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uANDUUn = unsigned & unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sANDUUn = unsigned & unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDUUn = unsigned & unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDUUn = unsigned & unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sANDUU = unsigned & unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDUU = unsigned & unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDUU = unsigned & unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sANDUC = unsigned & constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDUC = unsigned & constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDUC = unsigned & constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uANDSUn = signed & unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sANDSUn = signed & unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDSUn = signed & unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDSUn = signed & unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uANDSS = signed & signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDSS = signed & signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDSS = signed & signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uANDSC = signed & constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDSC = signed & constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDSC = signed & constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uANDCUn = constant & unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sANDCUn = constant & unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDCUn = constant & unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDCUn = constant & unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sANDCU = constant & unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDCU = constant & unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDCU = constant & unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uANDCS = constant & signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cANDCS = constant & signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDCS = constant & signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubANDCC = constant & constant;


		// Confirm that ^ follows the LUB rule for legal uses.
		//:: error: (assignment.type.incompatible)
		@Unsigned int uXORUnUn = unknown ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sXORUnUn = unknown ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORUnUn = unknown ^ unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORUnUn = unknown ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uXORUnU = unknown ^ unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sXORUnU = unknown ^ unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORUnU = unknown ^ unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORUnU = unknown ^ unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uXORUnS = unknown ^ signed;
		//:: error: (assignment.type.incompatible)
		@Signed int sXORUnS = unknown ^ signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORUnS = unknown ^ signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORUnS = unknown ^ signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uXORUnC = unknown ^ constant;
		//:: error: (assignment.type.incompatible)
		@Signed int sXORUnC = unknown ^ constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORUnC = unknown ^ constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORUnC = unknown ^ constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uXORUUn = unsigned ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sXORUUn = unsigned ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORUUn = unsigned ^ unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORUUn = unsigned ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sXORUU = unsigned ^ unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORUU = unsigned ^ unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORUU = unsigned ^ unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sXORUC = unsigned ^ constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORUC = unsigned ^ constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORUC = unsigned ^ constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uXORSUn = signed ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sXORSUn = signed ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORSUn = signed ^ unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORSUn = signed ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uXORSS = signed ^ signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORSS = signed ^ signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORSS = signed ^ signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uXORSC = signed ^ constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORSC = signed ^ constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORSC = signed ^ constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uXORCUn = constant ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sXORCUn = constant ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORCUn = constant ^ unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORCUn = constant ^ unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sXORCU = constant ^ unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORCU = constant ^ unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORCU = constant ^ unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uXORCS = constant ^ signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cXORCS = constant ^ signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORCS = constant ^ signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubXORCC = constant ^ constant;


		// Confirm that | follows the LUB rule for legal uses.
		//:: error: (assignment.type.incompatible)
		@Unsigned int uORUnUn = unknown | unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sORUnUn = unknown | unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cORUnUn = unknown | unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORUnUn = unknown | unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uORUnU = unknown | unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sORUnU = unknown | unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cORUnU = unknown | unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORUnU = unknown | unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uORUnS = unknown | signed;
		//:: error: (assignment.type.incompatible)
		@Signed int sORUnS = unknown | signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cORUnS = unknown | signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORUnS = unknown | signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uORUnC = unknown | constant;
		//:: error: (assignment.type.incompatible)
		@Signed int sORUnC = unknown | constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cORUnC = unknown | constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORUnC = unknown | constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uORUUn = unsigned | unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sORUUn = unsigned | unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cORUUn = unsigned | unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORUUn = unsigned | unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sORUU = unsigned | unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cORUU = unsigned | unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORUU = unsigned | unsigned;
		//:: error: (assignment.type.incompatible)
		@Signed int sORUC = unsigned | constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cORUC = unsigned | constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORUC = unsigned | constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uORSUn = signed | unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sORSUn = signed | unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cORSUn = signed | unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORSUn = signed | unknown;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uORSS = signed | signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cORSS = signed | signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORSS = signed | signed;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uORSC = signed | constant;
		//:: error: (assignment.type.incompatible)
		@Constant int cORSC = signed | constant;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORSC = signed | constant;

		//:: error: (assignment.type.incompatible)
		@Unsigned int uORCUn = constant | unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sORCUn = constant | unknown;
		//:: error: (assignment.type.incompatible)
		@Constant int cORCUn = constant | unknown;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORCUn = constant | unknown;
		//:: error: (assignment.type.incompatible)
		@Signed int sORCU = constant | unsigned;
		//:: error: (assignment.type.incompatible)
		@Constant int cORCU = constant | unsigned;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORCU = constant | unsigned;
		//:: error: (assignment.type.incompatible)
		@Unsigned int uORCS = constant | signed;
		//:: error: (assignment.type.incompatible)
		@Constant int cORCS = constant | signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORCS = constant | signed;
		//:: error: (assignment.type.incompatible)
		@UnsignednessBottom int ubORCC = constant | constant;
	}
}
