seq(jasmineSettings : _*)

appJsDir <+= resourceManaged { src => src / "main" / "public" }

appJsLibDir <+= baseDirectory { src => src / "public" }

jasmineTestDir <+= baseDirectory { src => src / "test" }

jasmineConfFile <+= baseDirectory { src => src / "test" /  "test.dependencies.js" }

jasmineRequireJsFile <+= baseDirectory { src => src / "test" / "require-2.0.6.js" }

jasmineRequireConfFile <+= baseDirectory { src => src / "test" / "require.conf.js" }

jasmine <<= jasmine dependsOn coffeetestTask

(test in Test) <<= (test in Test) dependsOn (jasmine)
