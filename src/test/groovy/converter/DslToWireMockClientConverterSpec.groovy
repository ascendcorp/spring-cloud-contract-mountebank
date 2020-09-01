package com.ascendcorp.contract.converter

import org.json.JSONObject
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.cloud.contract.spec.Contract
import org.springframework.cloud.contract.verifier.file.ContractMetadata
import org.springframework.cloud.contract.verifier.util.ContractVerifierDslConverter
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

class DslToWireMockClientConverterSpec extends Specification {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder()

    def "should convert DSL file to WireMock JSON"() {
        given:
            def converter = new MountebankConverter()
        and:
            File file = tmpFolder.newFile("dsl1.groovy")
            file.write("""
                    org.springframework.cloud.contract.spec.Contract.make {
                        request {
                            method('PUT')
                            url \$(consumer(~/\\/[0-9]{2}/), producer('/12'))
                        }
                        response {
                            status OK()
                        }
                    }
            """)
        when:
            String json = converter.convertContents("Test", new ContractMetadata(file.toPath(), false, 0, null,
                    ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
            JSONAssert.assertEquals('''
                {"predicates":[{"and":[{"matches":{"path":"\\/[0-9]{2}","method":"PUT"}}]}],"responses":[{"is":{"statusCode":"200"}}]}
            ''', json, false)
    }
    @Ignore
    def "should convert DSL file to WireMock JSON with byte arrays"() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl1.groovy")
        file.write("""
					[
			org.springframework.cloud.contract.spec.Contract.make {
				request {
					method "POST"
					url "/multipart"
					headers {
						contentType('multipart/form-data')
					}
					multipart(
							file: named(
									name: value(stub(regex('.+')), test('file')),
									content: value(stub(regex('.+')), test([100, 117, 100, 97] as byte[]))
							)
					)
				}
				response {
					status 200
					body "hello"
				}
			}
	]
""")
        when:
        String json = converter.convertContents("Test", new ContractMetadata(file.toPath(), false, 0, null,
                ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
        JSONAssert.assertEquals('''
{"predicates":[{"and":[{"equals":{"path":"\\/multipart","method":"POST"}},{"matches":{"headers":{"Content-Type":"multipart\\/form-data.*"}}}]}],"responses":[{"is":{"body":"hello","statusCode":"200"}}]}
''', json, false)
    }

    def "should convert DSL file with list of contracts to WireMock JSONs"() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl1_list.groovy")
        file.write('''
(1..2).collect { int index ->
	org.springframework.cloud.contract.spec.Contract.make {
		request {
			method(PUT())
			headers {
				contentType(applicationJson())
			}
			url "/${index}"
		}
		response {
			status OK()
		}
	}
}
''')
        when:
            Map<Contract, String> convertedContents = converter.convertContents("Test", new ContractMetadata(file.toPath(), false, 0, null,
                    ContractVerifierDslConverter.convertAsCollection(new File("/"), file)))
        then:
            convertedContents.size() == 2
            JSONAssert.assertEquals('''                
                {"predicates":[{"and":[{"equals":{"path":"\\/1","method":"PUT"}},{"matches":{"headers":{"Content-Type":"application\\/json.*"}}}]}],"responses":[{"is":{"body":"","statusCode":"200"}}]}
            ''', convertedContents.values().first(), false)
            JSONAssert.assertEquals('''
                {"predicates":[{"and":[{"equals":{"path":"\\/2","method":"PUT"}},{"matches":{"headers":{"Content-Type":"application\\/json.*"}}}]}],"responses":[{"is":{"body":"","statusCode":"200"}}]}
            ''', convertedContents.values().last(), false)
    }


    def "should not convert if contract is messaging related"() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl1_list.groovy")
        file.write('''
	(1..2).collect { int index ->
		org.springframework.cloud.contract.spec.Contract.make {
			input {
				triggeredBy("foo")
			}
		}
	}
	''')
        when:
        Map<Contract, String> convertedContents = converter.convertContents("Test",
                new ContractMetadata(file.toPath(), false, 0, null,
                        ContractVerifierDslConverter.convertAsCollection(new File("/"), file)))
        then:
        convertedContents.isEmpty()
    }

    def "should creation of delayed stub responses be possible"() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl-delay.groovy")
        file.write("""
				org.springframework.cloud.contract.spec.Contract.make {
					request {
						method 'GET'
						url '/foo'
					}
					response {
						status OK()
						fixedDelayMilliseconds 1000
					}
			}
""")
        when:
            String json = converter.convertContents("test", new ContractMetadata(file.toPath(), false, 0, null,
                ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
            JSONAssert.assertEquals('''
            {"predicates":[{"and":[{"equals":{"path":"\\/foo","method":"GET"}}]}],"responses":[{"_behaviors":{"wait":1000},"is":{"body":"","statusCode":"200"}}]}
            ''', json, false)
    }

    def "should convert DSL file with a nested list to WireMock JSON"() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl2.groovy")
        file.write("""
				org.springframework.cloud.contract.spec.Contract.make {
					request {
						method 'PUT'
						url '/api/12'
						headers {
							header 'Content-Type': 'application/vnd.org.springframework.cloud.contract.verifier.twitter-places-analyzer.v1+json'
						}
						body '''
					[{
						"created_at": "Sat Jul 26 09:38:57 +0000 2014",
						"id": 492967299297845248,
						"id_str": "492967299297845248",
						"text": "Gonna see you at Warsaw",
						"place":
						{
							"attributes":{},
							"bounding_box":
							{
								"coordinates":
									[[
										[-77.119759,38.791645],
										[-76.909393,38.791645],
										[-76.909393,38.995548],
										[-77.119759,38.995548]
									]],
								"type":"Polygon"
							},
							"country":"United States",
							"country_code":"US",
							"full_name":"Washington, DC",
							"id":"01fbe706f872cb32",
							"name":"Washington",
							"place_type":"city",
							"url": "https://api.twitter.com/1/geo/id/01fbe706f872cb32.json"
						}
					}]
				'''
					}
					response {
						status OK()
					}
				}
""")
        when:
        String json = converter.convertContents("Test", new ContractMetadata(file.toPath(), false, 0, null,
                ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
            JSONAssert.assertEquals('''
           {"predicates":[{"and":[{"equals":{"path":"/api/12","method":"PUT"}},{"equals":{"headers":{"Content-Type":"application/vnd.org.springframework.cloud.contract.verifier.twitter-places-analyzer.v1+json"}}},{"matches":{"body":[{"id_str":"492967299297845248","created_at":"Sat Jul 26 09:38:57 +0000 2014","id":492967299297845248,"text":"Gonna see you at Warsaw","place":{"country":"United States","country_code":"US","full_name":"Washington, DC","bounding_box":{"coordinates":[[[-77.119759,38.791645],[-76.909393,38.791645],[-76.909393,38.995548],[-77.119759,38.995548]]],"type":"Polygon"},"place_type":"city","name":"Washington","attributes":{},"id":"01fbe706f872cb32","url":"https://api.twitter.com/1/geo/id/01fbe706f872cb32.json"}}]}}]}],"responses":[{"is":{"body":"","statusCode":"200"}}]}           
            ''', json, false)
    }

    def "should create stub with map inside list"() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl-mapinlist.groovy")
        file.write("""
				org.springframework.cloud.contract.spec.Contract.make {
					request {
				method 'GET'
				urlPath '/foos'
			}
			response {
				status OK()
				body([[id: value(
						consumer('123'),
						producer(regex('[0-9]+'))
				)], [id: value(
						consumer('567'),
						producer(regex('[0-9]+'))
				)]])
				headers {
									header 'Content-Type': 'application/json'
								}
			}
			}
""")
        when:
        String json = converter.convertContents("test", new ContractMetadata(file.toPath(), false, 0, null,
                ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
            JSONAssert.assertEquals('''
            {"predicates":[{"and":[{"equals":{"path":"/foos","method":"GET"}}]}],"responses":[{"is":{"headers":{"Content-Type":"application/json"},"body":[{"id":"123"},{"id":"567"}],"statusCode":"200"}}]}
            ''', json, false)
    }

    def "should create stub when response has only one side of the dynamic value"() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl-dynamic.groovy")
        file.write("""
				org.springframework.cloud.contract.spec.Contract.make {
					request {
				method 'GET'
				urlPath '/foos'
			}
			response {
				status OK()
				body(
					digit: \$(producer(regex('[0-9]{1}'))),
					id: \$(producer(regex(number())))
				)
			}
			}
""")
        when:
            String json = converter.convertContents("test", new ContractMetadata(file.toPath(), false, 0, null,
                ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
            def jsonObj = new JSONObject(json)
            def jsonExpect = new JSONObject("{\"predicates\":[{\"and\":[{\"equals\":{\"path\":\"\\/foos\",\"method\":\"GET\"}}]}],\"responses\":[{\"is\":{\"body\":\"{\\\"id\\\":\\\"1.7\\\",\\\"digit\\\":\\\"1\\\"}\",\"statusCode\":\"200\"}}]}")
            JSONAssert.assertEquals(jsonExpect.get("predicates").toString(), jsonObj.get("predicates").toString(), false)
        and:
            def value = new JSONObject(jsonExpect.get("responses").get(0).get("is").get("body")).get("id").toString();
            def intValue = Double.parseDouble(value)
    }

    def 'should convert dsl to wiremock to show it in the docs'() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl_from_docs.groovy")
        file.write('''
			org.springframework.cloud.contract.spec.Contract.make {
				priority 1
				request {
					method 'POST'
					url '/users/password'
					headers {
						header 'Content-Type': 'application/json'
					}
					body(
							email: $(consumer(optional(regex(email()))), producer('abc@abc.com')),
							callback_url: $(consumer(regex(hostname())), producer('https://partners.com'))
					)
				}
				response {
					status 404
					headers {
						header 'Content-Type': 'application/json'
					}
					body(
							code: value(consumer("123123"), producer(optional("123123"))),
							message: "User not found by email == [${value(producer(regex(email())), consumer('not.existing@user.com'))}]"
					)
				}
			}
	''')
        when:
        String json = converter.convertContents("Test", new ContractMetadata(file.toPath(), false, 0, null,
                ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
            JSONAssert.assertEquals( // tag::wiremock[]
                '''
                {"predicates":[{"and":[{"equals":{"path":"/users/password","method":"POST"}},{"equals":{"headers":{"Content-Type":"application/json"}}},{"matches":{"body":{"callback_url":"((http[s]?|ftp):/)/?([^:/\\\\s]+)(:[0-9]{1,5})?","email":"([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,6})?"}}}]}],"responses":[{"is":{"headers":{"Content-Type":"application/json"},"body":{"code":"123123","message":"User not found by email == [not.existing@user.com]"},"statusCode":"404"}}]}
                '''
                , json, false)
    }

    def 'should convert dsl to wiremock with stub matchers'() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl_from_docs.groovy")
        file.write('''
			org.springframework.cloud.contract.spec.Contract.make {
				request {
					method 'POST'
					urlPath '/get'
					body([
							duck: 123,
							alpha: "abc",
							number: 123,
							aBoolean: true,
							date: "2017-01-01",
							dateTime: "2017-01-01T01:23:45",
							time: "01:02:34",
							valueWithoutAMatcher: "foo",
							valueWithTypeMatch: "string",
							list: [
								some: [
									nested: [
										json: "with value",
										anothervalue: 4
									]
								],
								someother: [
									nested: [
										json: "with value",
										anothervalue: 4
									]
								]
							],
							valueWithMin: [
								1,2,3
							],
							valueWithMax: [
								1,2,3
							],
							valueWithMinMax: [
								1,2,3
							]
					])
					bodyMatchers {
						jsonPath('$.duck', byRegex("[0-9]{3}"))
						jsonPath('$.duck', byEquality())
						jsonPath('$.alpha', byRegex(onlyAlphaUnicode()))
						jsonPath('$.alpha', byEquality())
						jsonPath('$.number', byRegex(number()))
						jsonPath('$.aBoolean', byRegex(anyBoolean()))
						jsonPath('$.date', byDate())
						jsonPath('$.dateTime', byTimestamp())
						jsonPath('$.time', byTime())
						jsonPath('$.list.some.nested.json', byRegex(".*"))
						jsonPath('$.valueWithMin', byType {
							// results in verification of size of array (min 1)
							minOccurrence(1)
						})
						jsonPath('$.valueWithMax', byType {
							// results in verification of size of array (max 3)
							maxOccurrence(3)
						})
						jsonPath('$.valueWithMinMax', byType {
							// results in verification of size of array (min 1 & max 3)
							minOccurrence(1)
							maxOccurrence(3)
						})
						jsonPath('$.valueWithOccurrence', byType {
							// results in verification of size of array (min 4 & max 4)
							occurrence(4)
						})
					}
					headers {
						contentType(applicationJson())
					}
				}
				response {
					status OK()
					body([
							duck: 123,
							alpha: "abc",
							number: 123,
							aBoolean: true,
							date: "2017-01-01",
							dateTime: "2017-01-01T01:23:45",
							time: "01:02:34",
							valueWithoutAMatcher: "foo",
							valueWithTypeMatch: "string",
							valueWithMin: [
								1,2,3
							],
							valueWithMax: [
								1,2,3
							],
							valueWithMinMax: [
								1,2,3
							],
							valueWithOccurrence: [
								1,2,3,4
							],
					])
					bodyMatchers {
						// asserts the jsonpath value against manual regex
						jsonPath('$.duck', byRegex("[0-9]{3}"))
						jsonPath('$.duck', byEquality())
						// asserts the jsonpath value against some default regex
						jsonPath('$.alpha', byRegex(onlyAlphaUnicode()))
						jsonPath('$.alpha', byEquality())
						jsonPath('$.number', byRegex(number()))
						jsonPath('$.aBoolean', byRegex(anyBoolean()))
						// asserts vs inbuilt time related regex
						jsonPath('$.date', byDate())
						jsonPath('$.dateTime', byTimestamp())
						jsonPath('$.time', byTime())
						// asserts that the resulting type is the same as in response body
						jsonPath('$.valueWithTypeMatch', byType())
						jsonPath('$.valueWithMin', byType {
							// results in verification of size of array (min 1)
							minOccurrence(1)
						})
						jsonPath('$.valueWithMax', byType {
							// results in verification of size of array (max 3)
							maxOccurrence(3)
						})
						jsonPath('$.valueWithMinMax', byType {
							// results in verification of size of array (min 1 & max 3)
							minOccurrence(1)
							maxOccurrence(3)
						})
						jsonPath('$.valueWithOccurrence', byType {
							// results in verification of size of array (min 4 & max 4)
							occurrence(4)
						})
					}
					headers {
						contentType(applicationJson())
					}
				}
			}
	''')
        when:
        String json = converter.convertContents("Test", new ContractMetadata(file.toPath(), false, 0, null,
                ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
        JSONAssert.assertEquals(//tag::matchers[]
                '''
{"predicates":[{"and":[{"equals":{"path":"/get","method":"POST"}},{"matches":{"headers":{"Content-Type":"application/json.*"}}},{"matches":{"body":{"date":"2017-01-01","dateTime":"2017-01-01T01:23:45","aBoolean":"true","list":"{some={nested={json=with value, anothervalue=4}}, someother={nested={json=with value, anothervalue=4}}}","valueWithMax":"[1, 2, 3]","number":"123","duck":"123","alpha":"abc","valueWithMin":"[1, 2, 3]","time":"01:02:34","valueWithTypeMatch":"string","valueWithMinMax":"[1, 2, 3]","valueWithoutAMatcher":"foo"}}}]}],"responses":[{"is":{"headers":{"Content-Type":"application/json"},"body":{"date":"2017-01-01","dateTime":"2017-01-01T01:23:45","aBoolean":"true","valueWithMax":"[1, 2, 3]","valueWithOccurrence":"[1, 2, 3, 4]","number":"123","duck":"123","alpha":"abc","valueWithMin":"[1, 2, 3]","time":"01:02:34","valueWithTypeMatch":"string","valueWithMinMax":"[1, 2, 3]","valueWithoutAMatcher":"foo"},"statusCode":"200"}}]}
''', json, false)
    }

    def 'should convert dsl to wiremock with stub matchers with docs example'() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl_from_docs.groovy")
        file.write('''
				org.springframework.cloud.contract.spec.Contract.make {
					priority 1
					request {
						method 'POST'
						url '/users/password2'
						headers {
							header 'Content-Type': 'application/json'
						}
						body(
							email: 'abc@abc.com',
							callback_url: 'https://partners.com'
						)
						bodyMatchers {
							jsonPath('$.[\\'email\\']', byRegex(email()))
							jsonPath('$.[\\'callback_url\\']', byRegex(hostname()))
						}
					}
					response {
						status 404
						headers {
							header 'Content-Type': 'application/json'
						}
						body(
							code: "123123",
							message: "User not found by email == [not.existing@user.com]"
						)
						bodyMatchers {
							jsonPath('$.code', byRegex("123123"))
							jsonPath('$.message', byRegex("User not found by email == ${email()}"))
						}
					}
				}
		''')
        when:
        String json = converter.convertContents("Test", new ContractMetadata(file.toPath(), false, 0, null,
                ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
            JSONAssert.assertEquals('''
            {"predicates":[{"and":[{"equals":{"path":"/users/password2","method":"POST"}},{"equals":{"headers":{"Content-Type":"application/json"}}},{"matches":{"body":{"callback_url":"https://partners.com","email":"abc@abc.com"}}}]}],"responses":[{"is":{"headers":{"Content-Type":"application/json"},"body":{"code":"123123","message":"User not found by email == [not.existing@user.com]"},"statusCode":"404"}}]}
            ''', json, false)
    }

    @Issue("#515")
    def 'should not escape any java chars in the javascript WireMock stub'() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl_from_docs.groovy")
        file.write('''
				org.springframework.cloud.contract.spec.Contract.make {
					priority 1
					request {
						method 'POST'
						url '/users/password2'
						headers {
							header 'Content-Type': 'application/json'
						}
						body(
							email: 'abc@abc.com',
							callback_url: 'https://partners.com'
						)
						bodyMatchers {
							jsonPath('$.[\\'email\\']', byRegex(email()))
							jsonPath('$.[\\'callback_url\\']', byRegex(hostname()))
						}
					}
					response {
						status 400
						headers {
							header 'CorrelationID': '11111111-1111-1111-1111-111111111111\'
							header 'Content-Type': value(test(regex('application/json(;.*)?')), stub('application/json;charset=UTF-8'))
						}
						body(
								[
										subject: [
												'@type'	:'ErrorSubject',
												'oid'		:'8.2',
												'description':'Profile'
										],
										reason : [
												'@type'	:'ErrorReason',
												'oid'		:'3.7',
												'description':'Bad Request',
												'httpCode':'400'
										],
										message: '[8.2 Profile/3.7 Bad Request]\'
								]
						)
					}
				}
		''')
        when:
        String json = converter.convertContents("Test", new ContractMetadata(file.toPath(), false, 0, null,
                ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
            JSONAssert.assertEquals('''
		    {"predicates":[{"and":[{"equals":{"path":"/users/password2","method":"POST"}},{"equals":{"headers":{"Content-Type":"application/json"}}},{"matches":{"body":{"callback_url":"https://partners.com","email":"abc@abc.com"}}}]}],"responses":[{"is":{"headers":{"CorrelationID":"11111111-1111-1111-1111-111111111111","Content-Type":"application/json;charset=UTF-8"},"body":{"reason":"{@type=ErrorReason, oid=3.7, description=Bad Request, httpCode=400}","subject":"{@type=ErrorSubject, oid=8.2, description=Profile}","message":"[8.2 Profile/3.7 Bad Request]"},"statusCode":"400"}}]}
	        ''', json, false)
    }

    @Issue("#449")
    def 'should properly convert regex for headers'() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl_from_docs.groovy")
        file.write('''
				org.springframework.cloud.contract.spec.Contract.make {
				request {
					method 'GET'
					urlPath($(
							consumer(regex('/v1/communities/(.+)/channels/[0-9]+')),
							producer('/v1/communities/contract/channels/1')))
			
					headers {
						header("X-Smartup-Test",
								$(
										consumer(regex(nonEmpty())),
										producer(1)))
					}
				}
				response {
					status 204
				}
			}
		''')
        when:
        String json = converter.convertContents("Test", new ContractMetadata(file.toPath(), false, 0, null,
                ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
        JSONAssert.assertEquals(
                '''
		{"predicates":[{"and":[{"matches":{"path":"\\/v1\\/communities\\/(.+)\\/channels\\/[0-9]+","method":"GET"}},{"matches":{"headers":{"X-Smartup-Test":"[\\\\S\\\\s]+"}}}]}],"responses":[{"is":{"body":"","statusCode":"204"}}]}
	'''
                , json, false)
    }

    def 'should properly convert for query parameters'() {
        given:
        def converter = new MountebankConverter()
        and:
        File file = tmpFolder.newFile("dsl_from_docs.groovy")
        file.write('''
				org.springframework.cloud.contract.spec.Contract.make {
                request {
                    method GET()
                    headers {
                        header('key', 'value')
                        contentType(applicationJson())
                    }
                    cookies {
                        cookie('another_key', 'another_value')
                    }
                    urlPath("/users") {
                        queryParameters {
                            parameter 'limit': 100
                            parameter 'filter': $(consumer(optional(regex("[email]"))), producer(''))
                            parameter 'gender': $(consumer(containing("[mf]")), producer('mf'))
                            parameter 'offset': $(consumer(matching("[0-9]+")), producer("1234"))
                        }
                    }
                }
                response {
                    status NOT_FOUND()
                    body(
                            status: 'success',
                            data: []
                    )
                }
			}
		''')
        when:
        String json = converter.convertContents("Test", new ContractMetadata(file.toPath(), false, 0, null,
                ContractVerifierDslConverter.convertAsCollection(new File("/"), file))).values().first()
        then:
        JSONAssert.assertEquals(
                '''
		{"predicates":[{"and":[{"equals":{"path":"/users","method":"GET","query":{"filter":"","limit":"100","offset":"1234","gender":"mf"}}},{"matches":{"headers":{"key":"value","Content-Type":"application/json.*"}}}]}],"responses":[{"is":{"body":{"data":"[]","status":"success"},"statusCode":"404"}}]}		
	'''
                , json, false)
    }


}
