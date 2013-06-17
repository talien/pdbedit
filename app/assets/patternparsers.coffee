patterndb_parsers = [
  name: "QSTRING"
  help: "Parse a string between the quote characters specified as parameter. Note that the quote character can be different at the beginning and the end of the quote, for example: @QSTRING::\"@ parses everything between two quotation marks (\"), while @QSTRING:&lt;&gt;@ parses from an opening bracket to the closing bracket. The @ character cannot be a quote character, nor can line-breaks or tabs."
,
  name: "ESTRING"
  help: "This parser has a required parameter that acts as the stopcharacter: the parser parses everything until it finds the stopcharacter. For example to stop by the next \" (double quote) character, use @ESTRING::\"@. To stop by a colon (:), the colon has to be escaped with another colon, like: @ESTRING::::@. As of syslog-ng OSE 3.1, it is possible to specify a stopstring instead of a single character, for example, @ESTRING::stop_here.@. The @ character cannot be a stopcharacter, nor can line-breaks or tabs."
,
  name: "STRING"
  help: "A sequence of alphanumeric characters (0-9, A-z), not including any whitespace. Optionally, other accepted characters can be listed as parameters (for example, to parse a complete sentence, add the whitespace as parameter, like: @STRING:: @). Note that the @ character cannot be a parameter, nor can line-breaks or tabs."
,
  name: "ANYSTRING"
  help: "Parses everything to the end of the message; you can use it to collect everything that is not parsed specifically to a single macro. In that sense its behavior is similar to the greedy() option of the CSV parser."
,
  name: "DOUBLE"
  help: "An obsolete alias of the @FLOAT@ parser."
,
  name: "EMAIL"
  help: "This parser matches an e-mail address. The parameter is a set of characters to strip from the beginning and the end of the e-mail address. That way e-mail addresses enclosed between other characters can be matched easily (for example, <user@example.com> or \"user@example.com\". Characters that are valid for a hostname are not stripped from the end of the hostname. This includes a trailing period if present.

For example, the @EMAIL:email:\"[<]>@ parser will match any of the following e-mail addresses: <user@example.com>, [user@example.com], \"user@example.com\", and set the value of the email macro to user@example.com."
,
  name: "FLOAT"
  help: "A floating-point number that may contain a dot (.) character. (Up to syslog-ng 3.1, the name of this parser was @DOUBLE@.)"
,
  name: "HOSTNAME"
  help: "Parses a generic hostname. The hostname may contain only alphanumeric characters (A-Z,a-z,0-9), hypen (-), or dot (.)."
,
  name: "IPv4"
  help: "Parses an IPv4 IP address (numbers separated with a maximum of 3 dots)."
,
  name: "IPv6"
  help: "Parses any valid IPv6 IP address."
,
  name: "IPvANY"
  help: "Parses any IP address."
,
  name: "LLADDR"
  help: "Parses a Link Layer Address in the xx:xx:xx:... form, where each xx is a 2 digit HEX number (an octet). The parameter specifies the maximum number of octets to match and defaults to 20. The MACADDR parser is a special wrapper using the LLADDR parser. For example, the following parser parses maximally 10 octets, and stores the results in the link-level-address macro:

@LLADDR:link-level-address:10@"
,
  name: "MACADDR"
  help: "Parses the standard format of a MAC-48 address, consisting of is six groups of two hexadecimal digits, separated by colons. For example, 00:50:fc:e3:cd:37."
,
  name: "NUMBER"
  help: "A sequence of decimal (0-9) numbers (for example, 1, 0687, and so on). Note that if the number starts with the 0x characters, it is parsed as a hexadecimal number, but only if at least one valid character follows 0x. A leading hyphen (–) is accepted for non-hexadecimal numbers, but other separator characters (for example, dot or comma) are not. To parse floating-point numbers, use the @FLOAT@ parser."
,
  name: "PCRE"
  help: "Use Perl-Compatible Regular Expressions (as implemented by the PCRE library), after the identification of the potential patterns has happened by the radix implementation.

Syntax: @PCRE:name:regexp@"
,
  name: "SET"
  help: "Parse any combination of the specified characters until another character is found. For example, specifying a whitespace character parses any number of whitespaces, and can be used to process paddings (for example, log messages of the Squid application have whitespace padding after the username).

For example, the @SET:: \"@ parser will parse any combination of whitespaces and double-quotes.

Available in syslog-ng OSE 3.4 and later."
]
