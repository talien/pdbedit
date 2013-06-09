define ["util"], (util) ->
  describe "is_parser",() ->
    it "should be the minimal parser", ->
      expect(is_parser("@::@")).toBe(true)

  describe "remove_from_array",() ->
    it "should remove an element", ->
      a = [1,2,3,4]
      remove_from_array(2, a)
      expect(a).toEqual([1,3,4])


  describe "split_by_delimiter",() ->
    it "should not split one token", ->
      str = "token"
      delimiters = " -;"
      expect(split_by_delimiters(str, delimiters)).toEqual([
        value: "token"
        index: 0
        type: "token"
      ])

    it "should split two tokens by delimiter", ->
      str = "token1 token2"
      delimiters = " "
      expect(split_by_delimiters(str, delimiters)).toEqual([
        value: "token1"
        index: 0
        type: "token"
      ,
        value: " "
        index: 1
        type: "delimiter"
      ,
        value: "token2"
        index: 2
        type: "token"
      ])

    it "should not split parsers", ->
     str = "@ESTRING:alma-bela:@"
     delimiters = " -"
     expect(split_by_delimiters(str, delimiters)).toEqual([
       value: "@ESTRING:alma-bela:@"
       index: 0
       type: "token"
     ])


    it "should handle two subsequent delimiters", ->
     str = "  "
     delimiters = " "
     expect(split_by_delimiters(str, delimiters)).toEqual([
        value: " "
        index: 0
        type: "delimiter"
      ,
        value: " "
        index: 1
        type: "delimiter"
     ])

  describe "tokenizer",() ->
    it "should construct Tokenizer", ->
     tokenizer = new Tokenizer(" ")
     expect(tokenizer.is_delimiter(" ")).toBe(true)
