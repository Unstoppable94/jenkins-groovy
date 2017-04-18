#!groovy
//timeout(time: 60, unit: 'SECONDS') {
node {
        def mvnHome = tool "${mavenId}"
        env.JAVA_HOME = tool "${jdk}"
        stage('��������') {
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
        stage('����') {
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
                stage(
                        "Findbugs") {
                    sh "'${mvnHome}/bin/mvn'  org.codehaus.mojo:findbugs-maven-plugin:3.0.4:check  -Dfindbugs.xmlOutput=true "
                }
                //if(env.Findbugs_continueOnfail=="true")
                //    sh "'${mvnHome}/bin/mvn'  org.codehaus.mojo:findbugs-maven-plugin:3.0.4:findbugs  -Dfindbugs.xmlOutput=true "
                //else
            } catch (exec) {
                if (env.Findbugs_continueOnfail == "true") {
                    
                    echo "set stage staatus"
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
                stage("Maven ����") {
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
            stage("������OSWAP���") {
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
                stage("������") {
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
            //�趨 jdk version
            //toDO ��ȡsonar ��Ϣ
            try {
                // withCredentials([usernameColonPassword(credentialsId: 'svn-winhong', variable: '')]) {
                // some block
                //    sh 'echo uname=$USERNAME pwd=$PASSWORD'
                //}
                //node('<MY_SLAVE>') {
                stage("����Sonar���") {
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
       
//Docker ϵ�в���
        if (env.CreateImage_skip == "false") {
            def imagename=env.CreateImage_registry+'/'+env.CreateImage_tag+":"+env.BUILD_TAG 
            echo imagename
            stage("��������") {
                sh "docker build -t ${imagename} ."
            }
            if (env.PushImage_skip == "false") {
                stage("Push����") {
//'${imagename}'
                    sh "docker push   '${imagename}' "
                    sh " echo '${imagename}' >dockerbuildresult.txt"
                    dirname="dockerbuildtempdir"+	System.currentTimeMillis()		
                    sh "mkdir '${dirname}' "
                    filename = java.net.URLEncoder.encode("image--"+imagename, "UTF-8")+".zip"
                    
                    archiveArtifacts filename
                    zip archive: true, dir:  dirname , glob: '', zipFile: filename
                    //sh "rm -rf  '${dirname}' "
                }
            }
        //test data
        //imagename="10.0.2.50/library/busybox"
            if (env.DeployToRancher_skip == "false") {
                stage("����Ӧ��") {
                try{
// �Ѿ����ڵķ�����Ҫ��down--todo
                    sh "rancher rm  '${DeployToRancher_service}' "
                }
                catch (exc) {
                }
                    sh "rancher run ${DeployToRancher_arg} --name '${DeployToRancher_service}' '${imagename}' ${DeployToRancher_cmd} "
                }
                
            }
        }
        
        currentBuild.result = 'SUCCESS'
    }

