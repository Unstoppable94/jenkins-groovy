#!groovy


def extMain()  {
int excuteTime = 30
try{
    excuteTime=env.maxExcutiontime .toInteger()}
catch(exc){
    excuteTime= 30
}
echo "excuteTime="+excuteTime
timeout(excuteTime){

        def mvnHome = tool "${mavenId}"
        env.JAVA_HOME = tool "${jdk}"
        stage('代码下载') {
            try {
                creid = "${SCMcredential}"
                //  echo  'CREDENTIALS'
                //  echo creid
            } catch (exc) {
                echo "Caught: ${exc}"
                creid = ""
            }
            //if (creid != null && creid != "") {
            echo ' credential id' + creid
            if (env.SCMTYPE == "git") {
                scmtype = 'GitSCM'
                echo 'using credential to git'
                git credentialsId: creid, url: env.SCMUrl, branch: env.SCMBranch
            } else { //svn
                echo 'using credential to svn'
                scmtype = 'SubversionSCM'
                checkout([$class: scmtype, additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[credentialsId: creid, depthOption: 'infinity', ignoreExternalsOption: true, local: '.', branches: [[name: '*/' + env.SCMBranch]],          \
                       remote: env.SCMUrl]], workspaceUpdater: [$class: 'UpdateUpdater']])
            }
//+env.SCMBranch
        }
        stage('编译') {
            CMD = "'${mvnHome}/bin/mvn' " + env.Compile_goal
            //-Dmaven.test.failure.ignore clean package"
            // CMD="'${mvnHome}/bin/mvn' -X " +env.Compile_goal
            echo CMD
            //sh "'${mvnHome}/bin/mvn' -Dmaven.test.failure.ignore clean package"
            sh CMD
            //sh "'${mvnHome}/bin/mvn' -X '${Compile_goal}'"
        }
        echo env.Findbugs_skip
        if (env.Findbugs_skip == "false") {
            //todo -Dfindbugs.includeFilterFile=./findbugsfilter.xml
            try {
                
                stage("Findbugs") {
                 	  findbugarg = ''
                 
            		if (env.Findbugs_inFilterUrl!='')
            			 findbugarg = " -Dfindbugs.includeFilterFile="+env.Findbugs_inFilterUrl
            		if (env.Findbugs_excludeFilterUrl!='')
            			 findbugarg = findbugarg+ " -Dfindbugs.excludeFilterFile="+env.Findbugs_excludeFilterUrl
            			 
            	    sh "'${mvnHome}/bin/mvn'  org.codehaus.mojo:findbugs-maven-plugin:3.0.4:check ${findbugarg} -Dfindbugs.xmlOutput=true "
                }
                //if(env.Findbugs_continueOnfail=="true")
                //    sh "'${mvnHome}/bin/mvn'  org.codehaus.mojo:findbugs-maven-plugin:3.0.4:findbugs  -Dfindbugs.xmlOutput=true "
                //else
            } catch (exec) {
                if (env.Findbugs_continueOnfail == "true") {
                    
                    echo "set stage status"
                    status result: "FAILURE"
                } else {
                    echo "exit "
                    sh "exit 1"
                    return
                }
            }
        }
        if (env.MavenTest_skip == "false") {
            //todo -Dfindbugs.includeFilterFile=./findbugsfilter.xml
            try {
                stage("Maven 测试") {
                    CMD = "'${mvnHome}/bin/mvn' " + env.MavenTest_goal
                    sh CMD
                }
            }
            catch (exc) {
                if (env.MavenTest_continueOnfail == "false") {
                                        throw exc
                    sh "exit 1"
                    return
                } else {
                    status result: "FAILURE"
                }
            }
        }
        if (env.OSWAPDepend_skip == "false") {
            //todo -Dfindbugs.includeFilterFile=./findbugsfilter.xml
            stage("依赖包OSWAP检查") {
                try {
                    sh "'${mvnHome}/bin/mvn'  org.owasp:dependency-check-maven:1.4.5:check -Ddependency-check-format=XML -DreportOutputDirectory=./target"
                }
                catch (exc) {
                    //TODO
                }
            }
        }
        if (env.Artifact_skip == "false") {
            //todo -Dfindbugs.includeFilterFile=./findbugsfilter.xml
            try {
                stage("打包结果") {
                    filename = env.BUILD_TAG + ".zip"
                    //echo filename
                    zip archive: true, dir: 'target', glob: '', zipFile: filename
                }
            }
            catch (exc) {
                if (env.Artifact_continueOnfail == "false") {
                    sh "exit 1"
                    return
                } else {
                    status result: "FAILURE"
                }
                //TODO
            }
        }
        if (env.Sonar_skip == "false") {
            //todo -Dfindbugs.includeFilterFile=./findbugsfilter.xml
            //设定 jdk version
            //toDO 获取sonar 信息
            //sonar 5.6 must use jdk1.8
             env.JAVA_HOME = tool "jdk1.8"
            try {
                // withCredentials([usernameColonPassword(credentialsId: 'svn-winhong', variable: '')]) {
                // some block
                //    sh 'echo uname=$USERNAME pwd=$PASSWORD'
                //}
                //node('<MY_SLAVE>') {
                stage("进行Sonar检查") {
                    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: env.SonarCredential,
                                      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                        echo "credentialsId"
                        echo env.USERNAME
                        sh "'${mvnHome}/bin/mvn'  org.sonarsource.scanner.maven:sonar-maven-plugin:3.0.2:sonar \
        -Dsonar.host.url='${env.SonarUrl}' -Dsonar.login='${env.USERNAME}' -Dsonar.password='${env.PASSWORD}'"
                    }
                    //}
                    //sh 'echo uname=$USERNAME pwd=$PASSWORD'
                }
            }
            catch (exc) {
                if (env.Sonar_continueOnfail == "false") {
                    throw exc
                    sh "exit 1"
                    return
                } else {
                    status result: "FAILURE"
                }
            }
        }
       
//Docker 系列操作
        if (env.CreateImage_skip == "false") {
            def imagename=env.CreateImage_registry+'/'+env.CreateImage_tag+":"+env.BUILD_TAG 
            echo imagename
            stage("创建镜像") {
                sh "docker build -t ${imagename} ."
                
                 sh " echo '${imagename}' >dockerbuildresult.txt"
                    dirname="dockerbuildtempdir"+	System.currentTimeMillis()		
                    sh "mkdir '${dirname}' "
                    filename = java.net.URLEncoder.encode("image--"+imagename, "UTF-8")+".zip"
                    
                    zip archive: true, dir:  dirname , glob: '', zipFile: filename
                    
            }
            if (env.PushImage_skip == "false") {
                stage("Push镜像") {
//'${imagename}'
                    sh "docker push   '${imagename}' "
                   
                    //sh "rm -rf  '${dirname}' "
                }
            }
        //test data
        //imagename="10.0.2.50/library/busybox"
            if (env.DeployToRancher_skip == "false") {
            

                    stage("部署应用") {
                    //sh "/usr/bin/rancher.sh ${DeployToRancher_arg}  '${DeployToRancher_service}' '${imagename}' ${DeployToRancher_cmd} "
                                    sh " export DeployToRancher_arg='${DeployToRancher_arg}'; \
                                    export DeployToRancher_cmd='${DeployToRancher_cmd}'; \
                                    export DeployToRancher_service='${DeployToRancher_service}'; \
                                    export imagename=${imagename}; \
                                    export DeployToRancher_environment=${DeployToRancher_environment}; \
                                    /usr/bin/rancher.sh "
                }

               
                
            }
        }
        
 //       currentBuild.result = 'SUCCESS'
    }
}

return this