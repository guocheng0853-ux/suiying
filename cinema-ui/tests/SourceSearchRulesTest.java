package com.guocheng.mediapocket;
import java.util.*;
public class SourceSearchRulesTest {
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 public static void main(String[] args){check(SourceSearchRules.keywords("审判萨达姆 高价值目标").contains("审判萨达姆"),"Chinese compound title fallback");check(SourceSearchRules.keywords("East of Eden").equals(Arrays.asList("East of Eden")),"do not search ambiguous single English word");check(SourceSearchRules.keywords("Silo.S02E04.mkv").equals(Arrays.asList("Silo")),"strip filename episode for search");check(SourceSearchRules.sameFilm("Silo","","Silo","2023"),"missing source year does not discard exact title");check(SourceSearchRules.sameFilm("Silo","2023年","Silo","2023–"),"normalize release year fields");check(!SourceSearchRules.sameFilm("Silo","2024","Silo","2023"),"known mismatched year rejected");check(!SourceSearchRules.sameFilm("Silo 2","","Silo","2023"),"similar title not auto-switched");System.out.println("PASS: alternative search keywords, missing/normalized release years and ambiguous-title rejection.");}
}
