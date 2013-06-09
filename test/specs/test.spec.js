define(["util"], function(util) {
  describe("is_parser", function() {
    return it("should be the minimal parser", function() {
      return expect(is_parser("@::@")).toBe(true);
    });
  });
  describe("remove_from_array", function() {
    return it("should remove an element", function() {
      var a;
      a = [1, 2, 3, 4];
      remove_from_array(2, a);
      return expect(a).toEqual([1, 3, 4]);
    });
  });
  describe("split_by_delimiter", function() {
    it("should not split one token", function() {
      var delimiters, str;
      str = "token";
      delimiters = " -;";
      return expect(split_by_delimiters(str, delimiters)).toEqual([
        {
          value: "token",
          index: 0,
          type: "token"
        }
      ]);
    });
    it("should split two tokens by delimiter", function() {
      var delimiters, str;
      str = "token1 token2";
      delimiters = " ";
      return expect(split_by_delimiters(str, delimiters)).toEqual([
        {
          value: "token1",
          index: 0,
          type: "token"
        }, {
          value: " ",
          index: 1,
          type: "delimiter"
        }, {
          value: "token2",
          index: 2,
          type: "token"
        }
      ]);
    });
    it("should not split parsers", function() {
      var delimiters, str;
      str = "@ESTRING:alma-bela:@";
      delimiters = " -";
      return expect(split_by_delimiters(str, delimiters)).toEqual([
        {
          value: "@ESTRING:alma-bela:@",
          index: 0,
          type: "token"
        }
      ]);
    });
    return it("should handle two subsequent delimiters", function() {
      var delimiters, str;
      str = "  ";
      delimiters = " ";
      return expect(split_by_delimiters(str, delimiters)).toEqual([
        {
          value: " ",
          index: 0,
          type: "delimiter"
        }, {
          value: " ",
          index: 1,
          type: "delimiter"
        }
      ]);
    });
  });
  return describe("tokenizer", function() {
    return it("should construct Tokenizer", function() {
      var tokenizer;
      tokenizer = new Tokenizer(" ");
      return expect(tokenizer.is_delimiter(" ")).toBe(true);
    });
  });
});