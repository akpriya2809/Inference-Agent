package assignment3;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class homework {
	int numQueries;
	static ArrayList<String> queries = new ArrayList<>();
	int numKB;
	static ArrayList<String> knowledgeBase = new ArrayList<>();
	HashMap<String, HashSet<String>> wholeKB = new HashMap<>(); //pred, list of questions
	static HashMap<Pattern, Integer> tokenMap = new HashMap<>();
	static ArrayList<String> tokens = new ArrayList<>();
	static LinkedList<String> s = new LinkedList<>();
	static int negCount = 0, andCount = 0, orCount = 0, implCount = 0;
	static LinkedList<String> predicate = new LinkedList<>();
	static LinkedList<LinkedList<String>> sentences = new LinkedList<>();
	static ArrayList<String> answersList = new ArrayList<>();
	static LinkedList<String> KBList = new LinkedList<>();
	static boolean[] varArray = new boolean[26];
	static char[] varList = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};

	public static void main(String[] args) {
		String ptn = "\\(";
		tokenMap.put(Pattern.compile("^("+ptn+")"), 1);
		ptn ="\\)";
		tokenMap.put(Pattern.compile("^("+ptn+")"), 2);
		ptn = "\\,";
		tokenMap.put(Pattern.compile("^("+ptn+")"), 3);
		ptn ="\\#";
		tokenMap.put(Pattern.compile("^("+ptn+")"), 4);
		ptn ="\\&";
		tokenMap.put(Pattern.compile("^("+ptn+")"), 5);
		ptn = "\\|";
		tokenMap.put(Pattern.compile("^("+ptn+")"), 6);
		ptn = "\\~";
		tokenMap.put(Pattern.compile("^("+ptn+")"), 2);
		ptn = "[a-zA-Z][a-zA-Z0-9_]*";
		tokenMap.put(Pattern.compile("^("+ptn+")"), 2);
		

		
		
		File file = new File("/Users/akankshapriya/AI_Assignments/Homework3/src/input.txt");
		Scanner sc;
		homework hw= new homework();
		try {
			sc = new Scanner(file);
			if(sc.hasNextLine()) {
				hw.numQueries = Integer.parseInt(sc.nextLine());
			}
			int n = hw.numQueries;
			
			while(sc.hasNextLine() && n>0) {
				queries.add(sc.nextLine().trim());
				n--;
			}
			//System.out.println(queries);
			if(sc.hasNextLine()) {
				hw.numKB = Integer.parseInt(sc.nextLine());
			}
			n = hw.numKB;
			while(sc.hasNextLine() && n>0) {
				knowledgeBase.add(sc.nextLine().trim());
				n--;
			}
			String questions[][] = new String[hw.numQueries][4];
			
			answersList.clear();
			for(int i = 0; i<hw.numQueries; i++) {
				hw.wholeKB.clear();
				sentences.clear();
				KBList.clear();
				Arrays.fill(varArray, false);
				
				
				questions[i][0] = queries.get(i); // complete query
				questions[i][1] = Negate(queries.get(i));// negated complete query
				// store predicate with and without negation
				questions[i][2] = processQuery(questions[i][0]); // predicate
				questions[i][3] = processQuery(questions[i][1]);// negated predicate
				
				
				if(hw.wholeKB.containsKey(questions[i][3]))
                {
                   HashSet<String> set = hw.wholeKB.get(questions[i][2]);
                   set.add(questions[i][1]);
                   hw.wholeKB.put(questions[i][3], set);
                   KBList.add(questions[i][1]);

                }
                else
                {
                	HashSet<String> tempqts= new HashSet<>();
                	tempqts.add(questions[i][1].replaceAll("\\s+", ""));
                	hw.wholeKB.put(questions[i][3],tempqts);
                	KBList.add(questions[i][1].replaceAll("\\s+", ""));
                }
				//printKB(hw.wholeKB);
				for(int j =0; j<hw.numKB; j++) {
					generateTokens(knowledgeBase.get(j).replaceAll("\\s+", "").replaceAll("=>", "#"));
					LinkedList<String> preds = new LinkedList<>(predicate);
					sentences.add(preds);
				}
				addPredstoKB(hw.wholeKB, true);
				
				boolean  ans = getAnswer(hw.wholeKB);
				System.out.println("ans::"+ans);
				
			}	
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
	private static boolean getAnswer(HashMap<String, HashSet<String>> wholeKB) {
		System.out.println("KBList::"+KBList);
		int num =0;
		while(true) {
			
			System.out.println("-------------------------------------------------------");
			ArrayList<String> newSentences = new ArrayList<>();
			for(int i = 0; i<KBList.size(); i++) {
				for(int j =i+1; j<KBList.size(); j++) {
					String[] arr1 = KBList.get(i).split("\\|");
					ArrayList<String> firstList = new ArrayList<>(Arrays.asList(arr1));
					String[] arr2 = KBList.get(j).split("\\|");
					ArrayList<String> secondList = new ArrayList<>(Arrays.asList(arr2));
					HashMap<String, String> substitutionList = new HashMap<>();
//					System.out.println("i:"+i+"j::"+j);
					//System.out.println("num::"+num+"firstList"+firstList);
					//System.out.println("secondList"+secondList);
					boolean isUnify = resolve(firstList, secondList, substitutionList);
					if(isUnify) {
						firstList.addAll(secondList);
						if(firstList.size()==0 ) {
							return true;
						}else {
							if(!substitutionList.isEmpty()) {
								replaceVariables(firstList, substitutionList);
							}
							ArrayList<String> temp = new ArrayList<>();
							for(String tmp:firstList) {
								if(!temp.contains(tmp)) {
									temp.add(tmp);
								}
							}
							String finalStr = String.join("|", temp);
							newSentences.add(finalStr);
						}
					}
					
				}
			}
			//System.out.println("newSentences"+newSentences);
			
			sentences.clear();
			boolean isAllPresent = addNewSentencestoKBList(newSentences);
			if(isAllPresent) {
				 return false;
				 
			}
			System.out.println("Round:"+num);
			for(String kb: KBList) {
				System.out.println(kb);
			}
			
			for(int t = 0; t<newSentences.size(); t++) {
				generateTokens(newSentences.get(t));
				LinkedList<String> preds = new LinkedList<>(predicate);
				sentences.add(preds);
			}
			addPredstoKB(wholeKB, false);
			
			
			
			num++;
			
			
		}
		
	}


	private static boolean resolve(ArrayList<String> alphaList, ArrayList<String> arrList,HashMap<String, String> substitutionList ) {
		boolean atLeastOneUnify = false;
		//System.out.println("alphaList"+alphaList);
		//System.out.println("arrList"+arrList);
		for(int i =0; i <alphaList.size(); i++) {
			String premise = alphaList.get(i);
			String pred = processQuery(alphaList.get(i));
			String negatedPred = Negate(pred);
			LinkedList<String> args1=findArgs(premise);
			for(int j = 0; j<arrList.size();j++) {
				//System.out.println("i:"+i+"j:"+j);
				String arrPred = processQuery(arrList.get(j));
				//System.out.println(arrPred.equals(negatedPred));
				if(arrPred.equals(negatedPred)) {
					LinkedList<String> args2=findArgs(arrList.get(j));
					if(args1.size()==args2.size()) {
						
						if(alphaList.size()!=0  && arrList.size()!=0) {
							boolean isUnify = doUnification(alphaList.get(i), arrList.get(j), args1, args2, substitutionList);
							if(isUnify) {
								atLeastOneUnify = true;
								arrList.remove(j);
								alphaList.remove(i);
								break;
							}
						}
						
					}
					
				}
				
			}
		}
		return atLeastOneUnify;

	}
	private static boolean addNewSentencestoKBList(ArrayList<String> newSentences) {
		boolean isAllPresent = false;
		int count = 0;
		for(String s: newSentences) {
			if(KBList.contains(s)) {
				count++;
			}else {
				KBList.add(s);
			}
			
		}
		if(count == newSentences.size()) {
			isAllPresent = true;
		}
		return isAllPresent;
	
	}
	private static void replaceVariables(ArrayList<String> arrList, HashMap<String, String> substitutionList) {
		for(int t = 0; t<arrList.size();t++) {
			String s = arrList.get(t);
			int ch =0;
			String tmp="", newStr="";
			//int index = s.indexOf('(');
			//index++;
			LinkedList<String> args1=findArgs(s);
			for(String arg: args1) {
				if(substitutionList.containsKey(arg)) {
					int index = s.indexOf(arg);
					newStr = s.substring(0,index)+substitutionList.get(arg)+s.substring(index+1, s.length());
					arrList.remove(t);
					arrList.add(t, newStr);	
				}
			}
			
//			while(index <str.length()) {
//				if((str.charAt(index)!=',') && (str.charAt(index)!=')')) {
//					tmp+=str.charAt(index);
//				}else {
//					if(substitutionList.containsKey(tmp)) {
//					newStr = str.substring(0,index-1)+substitutionList.get(tmp)+s.substring(index, s.length());
//					arrList.remove(t);
//					str=newStr;
//					arrList.add(t, newStr);				
//					}
//					tmp="";
//				}
//				index++;
//			}
		}
		
	}


	private static boolean  doUnification( String premise, String kbstr, LinkedList<String> args1, 
			LinkedList<String> args2, HashMap<String, String> substitutionList) {
		int len = args1.size();
		//boolean isUnify = true;
		for(int i =0; i<len ; i++) {
			if(isConstant(args1.get(i)) && isConstant(args2.get(i))){
				if(!args1.get(i).equals(args2.get(i))) {
					return false;
				}
			}else if(!isConstant(args1.get(i)) && isConstant(args2.get(i))) {
				substitutionList.put(args1.get(i), args2.get(i));
				//unify conclusion with args1
			}else if(isConstant(args1.get(i)) && !isConstant(args2.get(i))) {
				substitutionList.put(args2.get(i), args1.get(i));
			}else if(!isConstant(args1.get(i))&& !isConstant(args2.get(i))){
				//standardize
				if(!args1.get(i).equals(args2.get(i))) {
					return false;
				}
				
			}
		}
		return true;
	}

	private static boolean isConstant(String string) {
		if(Character.isUpperCase(string.charAt(0))) {
			return true;
		}
		return false;
	}

	private static LinkedList<String> findArgs(String str) {
		LinkedList<String> argList = new LinkedList<>();
		int i = 0;
		while(str.charAt(i) !='(') {
			
			i++;
		}
		String temp="";
		for(int j = i+1; j<str.length(); j++) {
			char ch = str.charAt(j);
			if(ch == ',') {
				argList.add(temp);
				temp ="";
			}else if (ch == ')'){
				argList.add(temp);
				break;
			}else {
				temp+=ch;
			}
			
		}
		return argList;
	}

	private static void generateTokens(String str) {
		String s = str.trim();
		tokens.clear();
		predicate.clear();
		while(!s.equals("")) {
			
			for(Pattern p :tokenMap.keySet()) {
				Matcher m = p.matcher(s);
				if(m.find()) {
					//check for case when not matched
		        	String tok = m.group().trim();
			        s = m.replaceFirst("").trim();
			        tokens.add(tok);
			        break;
				}
			}
		}
		
		parser();

	}

	private static void parser() {
		
		int i = 0, j=0;
		String temp="";
		int l = tokens.size();
		Map<Character, Character> varSubsMap = new HashMap<>();
		HashSet<String> set = new HashSet<>();
		while(i <l) {
			String tok = tokens.get(i);
			if(tok.equals("&")) {
				andCount++;
				temp+=tok;
			}
			if(tok.equals("~")) {
				negCount++;
				temp+=tok;
			}
			if(tok.equals("|")) {
				orCount++;
				temp+=tok;
			}
			if(tok.equals("#")) {
				implCount++;
				temp+=tok;
			}
			
			if(Character.isUpperCase(tok.charAt(0))) {
				
				temp+=tok;
				 j = i+1;
				while(j<l) {
					tok = tokens.get(j);
					if(!isConstant(tok) && Character.isLetter(tok.charAt(0))) {
						if(varArray[tok.charAt(0)-'a']) {
							Character subs = varSubsMap.get(tok.charAt(0));
								if(subs!=null) {
									tok = subs+"";
									set.add(tok);
								}else {
									for(int k =0; k<26;k++) {
										if(!varArray[k]) {
											varSubsMap.put(tok.charAt(0),varList[k]);
											
											tok=varList[k]+"";
											varArray[tok.charAt(0)-'a']=true;
											set.add(tok);
											break;
										}
									}
								}
							
							
						}else {
							//varArray[tok.charAt(0)-'a']=true;
							set.add(tok);
						}
					}
					if(tok.equals(")")){
						temp+=tok;
						break;
					}else {
						temp+=tok;
					}
					j++;
				}
				predicate.add(temp);
				temp="";
				i=j+1;
			}else {
				i++;
				predicate.add(temp);
				temp="";
			}
				

		}
		for(String var: set) {
			varArray[var.charAt(0)-'a']=true;
		}
		varSubsMap.clear();
		if(implCount>0) {
			removeImplySign(implCount);
		}
		
		//System.out.println("without imply predicate:::"+predicate);	
		
	}

	private static void addPredstoKB(HashMap<String, HashSet<String>> wholeKB, boolean fromInitial) {
		String temp ="";
		
				for(LinkedList<String> predlist:sentences) {
					for(String s: predlist) {
						if(s.equals("~")) {
							temp+=s;
							
						}
						if (Character.isUpperCase(s.charAt(0))) {
							
							int i =0;
							while(s.charAt(i)!='(') {
								temp+=String.valueOf(s.charAt(i));
								i++;
							}
							if(wholeKB.containsKey(temp) ) {
								HashSet<String> list = wholeKB.get(temp);
								StringBuilder sb  = new StringBuilder();
								for(String st:predlist) {
									sb.append(st);
								}
								if(!KBList.contains(sb.toString()) && fromInitial) {
									KBList.add(sb.toString());
								}
								list.add(sb.toString());
								wholeKB.put(temp, list);
								
								temp="";
							}else {
								HashSet<String> newList = new HashSet<>();
								StringBuilder sb  = new StringBuilder();
								for(String st:predlist) {
									sb.append(st);
								}
								if(!KBList.contains(sb.toString()) && fromInitial) {
									KBList.add(sb.toString());
								}
								newList.add(sb.toString());
								wholeKB.put(temp,newList);
								temp="";
								
							}
						}
					}
				}	
		
		
	}

	private static void removeImplySign(int count) {
		 int lhs = -1;
	        int l = 0;
	            lhs = 0;
	            LinkedList<String> temp = new LinkedList<>();
	            int check = 0;
	            for(int i =0 ; i<predicate.size(); i++) {
            	if(predicate.get(i).equals("#")) {
            		predicate.remove(i);
            		implCount --;
            		predicate.add(i, "|");
            		lhs=i;  
            		break;
            	}
            }
	            for(int i =0; i<lhs;i++) {
	            	String str = predicate.get(i);
	            	if(str.equals("~")) {
	            		check++;
	            	}else if(str.equals("&")) {
	            		temp.add("|");
	            	}else if (check>0) {
	            		check--;
	            		temp.add(str);
	            	}else {
	            		temp.add("~");
	            		temp.add(str);
	            	}
	            }
	            while(lhs<predicate.size()){
	            	temp.add(predicate.get(lhs));
	            	lhs++;
	            }
	            predicate = (LinkedList<String>)temp.clone();

//	            for(int i =lhs-1 ; i>=0; i--) {
//	            	if(!predicate.get(i).equals("&")) {
//	            		predicate.add(i,"~");
//	            	}else {
//	            		predicate.remove(i);
//	            		predicate.add(i, "|");
//	            	}
//	            }
	        
		
	}

	private static String processQuery(String query) {
		char[] arr = query.toCharArray();
		int i=0;
		String returnstr="";
    	while(arr[i]!='(')
    	{
    		returnstr=(returnstr+String.valueOf(arr[i]));
    		i++;
    	}
    	return returnstr;
		
	}
	private static  String Negate(String query) {
		query = query.trim();
		String str="";
		if(query.charAt(0)== '~') {
			 str = query.substring(1);
		}else {
			str ='~'+query;
		}
		return str;
		
	}
	private static void printKB(HashMap<String, HashSet<String>> map ) {
		for (String name: map.keySet()) {
			HashSet<String> list = map.get(name);
			//for(int i = 0; i<list.size();i++) {
				//System.out.println("name:"+name+"Set"+list);
			//}
			
		}
	}

}
