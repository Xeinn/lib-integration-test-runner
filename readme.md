#Dependency
```
	<dependency>
		<groupId>uk.co.xeinn.pfref</groupId>
		<artifactId>lib-integration-test-runner</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</dependency>
```

#Usage

Create an empty spring boot project and create a unit test and add the following;

```
package uk.co.xeinn.sampletest;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.xeinn.pfref.testing.integration.IntegrationScriptRunner;

public class SampleTest {

	private static final Logger logger = LoggerFactory.getLogger(SampleTest.class);

	private static final IntegrationScriptRunner script = new IntegrationScriptRunner();
	
	@Test
	public void immediateSuccessfulOneStage( ) {
		assertTrue(script.runScript("my-test-script.http"));
	}

```

My test script can be any rest client format file with test conditions coded into comments as follows

```

###
# My Test
# 
# The commands and tests below are executed either before or after the rest call that directly follows them
#
# The test runner library picks up on the #> characters at the beginning of the line and interprets matching commands that follow e.g.
#
#> Set @returnedvalue = result.value
#
#> Expect result status 200
#
POST http://localhost/myservice HTTP/1.1
myheader: {{headerval}}
Content-Type: application/json

{
    "some": {
        "test": "data"
    }
}


```

possible commands are

***Expect result status = NNN NNN NNN***

Where NNN is an http response code

```
Example:

#> Expect result status = 200
```

***Expect value.from.response = "regex" | 'regex' | regex***

```
Example:

#> Expect result.myvalue = .*
```

***Expect value.from.response [is] not present***

```
Example:

#> Expect result.myvalue is not present
```

***Expect header <headername>***

```
Example:

#> Expect header Content-Type
```

***Expect header <headername> value =  "regex" | 'regex' | regex***

```
Example:

#> Expect header Content-Type value = "application/json"
```

***Wait [before [running [for]]] NNN s | seconds***

Causes a delay of NNN seconds before the following rest request is executed.

