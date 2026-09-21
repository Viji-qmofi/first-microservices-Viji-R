"""Unit tests for fts_query() stopword filtering in server.py.

server.py is deliberately NOT imported: importing it runs FastMCP("retrieval")
and pulls in sentence_transformers, sqlite_vec and starlette, whose
import-time failures are opaque. Instead the loader below parses the real
source text with ast, extracts only the KEYWORD_STOPWORDS assignment and the
fts_query function, and execs them in a namespace containing only `re`. This
tests the real source, not a copy, and an unexpected dependency surfaces as
a NameError.

Run from /workspace/mcp/retrieval:  python3 -m unittest -v test_fts_query
"""
import ast
import os
import re
import unittest

SERVER_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "server.py")


def _load_symbols(path):
    with open(path, "r", encoding="utf-8") as handle:
        tree = ast.parse(handle.read(), filename=path)

    assigns = []
    funcs = []
    for node in tree.body:
        if (
            isinstance(node, ast.Assign)
            and len(node.targets) == 1
            and isinstance(node.targets[0], ast.Name)
            and node.targets[0].id == "KEYWORD_STOPWORDS"
        ):
            assigns.append(node)
        elif isinstance(node, ast.FunctionDef) and node.name == "fts_query":
            funcs.append(node)

    for name, found in (("KEYWORD_STOPWORDS", assigns), ("fts_query", funcs)):
        if not found:
            raise RuntimeError(f"symbol {name!r} not found at top level of {path}")
        if len(found) > 1:
            raise RuntimeError(f"symbol {name!r} defined more than once in {path}")

    module = ast.Module(body=[assigns[0], funcs[0]], type_ignores=[])
    ast.fix_missing_locations(module)
    code = compile(module, path, "exec")
    namespace = {"re": re}
    exec(code, namespace)

    fts = namespace.get("fts_query")
    stopwords = namespace.get("KEYWORD_STOPWORDS")
    if not callable(fts):
        raise RuntimeError("loader failed: fts_query is not callable")
    if not isinstance(stopwords, frozenset) or not stopwords:
        raise RuntimeError("loader failed: KEYWORD_STOPWORDS is not a non-empty frozenset")
    return fts, stopwords


fts_query, KEYWORD_STOPWORDS = _load_symbols(SERVER_PATH)


class FillerWordExclusionTests(unittest.TestCase):
    def test_question_sentence_drops_filler(self):
        self.assertEqual(
            fts_query("what are the cost ceiling rules for this"),
            '"ceiling" OR "rules" OR "cost"',
        )

    def test_stopword_filter_is_case_insensitive_and_keeps_casing(self):
        self.assertEqual(fts_query("The Cost"), '"Cost"')

    def test_every_stopword_alone_yields_empty(self):
        for word in sorted(KEYWORD_STOPWORDS):
            with self.subTest(word=word):
                self.assertEqual(fts_query(word), "")
            with self.subTest(word=word.upper()):
                self.assertEqual(fts_query(word.upper()), "")

    def test_identifiers_kept(self):
        self.assertEqual(
            fts_query("OrderServiceImpl Feign"), '"OrderServiceImpl" OR "Feign"'
        )

    def test_hyphen_is_not_a_token_character(self):
        self.assertEqual(fts_query("MCP -32000"), '"32000" OR "MCP"')

    def test_underscore_identifier_is_split_in_second_pass(self):
        self.assertEqual(fts_query("my_var"), '"my_var" OR "var" OR "my"')

    def test_characterization_stopword_inside_underscore_identifier(self):
        # Characterization of current behavior, not a stated requirement:
        # the whole token "the_cost" is not a stopword so it is kept, while
        # the split pass drops "the" and keeps "cost".
        self.assertEqual(fts_query("the_cost"), '"the_cost" OR "cost"')

    def test_format_quoted_and_joined_with_or(self):
        result = fts_query("alpha bravo12 c")
        self.assertEqual(result, '"bravo12" OR "alpha" OR "c"')
        for term in result.split(" OR "):
            self.assertTrue(term.startswith('"') and term.endswith('"'))

    def test_case_variants_compared_as_set(self):
        # Order of case-only variants depends on set iteration, so compare
        # as a set of terms.
        result = fts_query("Cost cost")
        self.assertEqual(set(result.split(" OR ")), {'"Cost"', '"cost"'})


class AllFillerAndEmptyTests(unittest.TestCase):
    def test_all_filler_short(self):
        self.assertEqual(fts_query("what is this"), "")

    def test_all_filler_long_mixed_case_and_punctuation(self):
        # Every word verified present in KEYWORD_STOPWORDS.
        text = "How could this be, if it is NOT the?"
        self.assertEqual(fts_query(text), "")

    def test_empty_string(self):
        self.assertEqual(fts_query(""), "")

    def test_whitespace_only(self):
        self.assertEqual(fts_query("   \t\n "), "")

    def test_punctuation_only(self):
        self.assertEqual(fts_query("?!..."), "")

    def test_result_is_str(self):
        for text in ("", "what is this", "cost"):
            with self.subTest(text=text):
                self.assertIsInstance(fts_query(text), str)


class StopwordSetGuardTests(unittest.TestCase):
    def test_is_non_empty_frozenset(self):
        self.assertIsInstance(KEYWORD_STOPWORDS, frozenset)
        self.assertTrue(KEYWORD_STOPWORDS)

    def test_no_domain_terms(self):
        for word in ("cost", "order", "ceiling"):
            with self.subTest(word=word):
                self.assertNotIn(word, KEYWORD_STOPWORDS)


if __name__ == "__main__":
    unittest.main()
