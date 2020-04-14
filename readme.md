#Dependency
```
	<dependency>
		<groupId>com.pay360.pfref</groupId>
		<artifactId>lib-jwt-authentication</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</dependency>
```

#Properties

**com.pay360.pfref.jwt.authentication.header-name**

The header in which the authentication jwt is expected to be found.  If empty then authentication via jwt header is disabled.  jwt must have claim with value that contains the string "cookie" indicating that the jwt has been granted the ability to be passed by cookie for authentication.

Default value is "jwt"

**com.pay360.pfref.jwt.authentication.cookie-name**

The cookie in which the authentication jwt is expected to be found. If empty then authentication via jwt header is disabled.  jwt must have claim with value that contains the string "cookie" indicating that the jwt has been granted the ability to be passed by cookie for authentication.

Default value is "jwt"

#Configuration

The example below shows the configuration of the jwt-authentication library for a typical service that wants to session-less authentication using the JwtAuthenticationSecurityConfigurer.

An alternative would be to manually configure the filter the default JwtAuthenticationFilter bean provided by the library at the relevant point in the filter chain. 

``` Java
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	private JwtAuthenticationSecurityConfigurer jwtAuthConfigurer;
	
	public WebSecurityConfig(JwtAuthenticationSecurityConfigurer jwtAuthConfigurer) {
		this.jwtAuthConfigurer = jwtAuthConfigurer;
	}
	
	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.requestMatchers().antMatchers("/**");
		http.apply(jwtAuthConfigurer);
		http.authorizeRequests().anyRequest().authenticated();
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		http.cors().disable().csrf().disable();
	}
	
	@Override
	public AuthenticationManager authenticationManager() {
		return (Authentication auth) -> null;
	}
}
```