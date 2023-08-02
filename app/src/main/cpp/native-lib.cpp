#include <jni.h>
#include <sstream>
#include <iostream>
#include <random>
#include <string>
#include <fstream>
#include <unordered_map>
#include <stdexcept>
#include <android/log.h>

namespace parser{
    typedef std::string  string;
    struct VCD_var {
        char symbol;
        string scope;
        string type;
        int size;
        int val;
    };

    int get_timescale(std::stringstream & file){
        int timescale;
        string token = "";

        const string time_scale_token = "$timescale";

        while(token != time_scale_token && !file.eof()){
            file >> token;
        }
        if(file.eof()) throw std::invalid_argument("FILE ENDED TOO SOON");
        file >> token;
        sscanf(token.c_str(), "%d", &timescale);

        return timescale;
    }

    void get_vars(std::stringstream & file, std::unordered_map<string, std::vector<VCD_var>> &scopes, std::unordered_map<char, std::vector<VCD_var>> &symbols){
        string token = "";
        string var_scope = "";
        //std::unordered_map<char  , std::vector<VCD_var>> symbols;
        //std::unordered_map<string, std::vector<VCD_var>> scopes;

        while(token != "$enddefinitions" && !file.eof()){
            file >> token;
            if(token == "$scope"){
                string type;
                file >> type; //don't actually have use for it yet
                string scope;
                file >> scope;
                var_scope += "." + scope;

            }else if(token == "$var"){
                //$var wire 1 ! result $end
                string type;
                int size;
                char symbol;
                string name;
                file >> type >> size >> symbol >> name;
                name = var_scope + "." + name;


                if(scopes.find(var_scope) == scopes.end()){
                    scopes[var_scope] = {};
                }

                VCD_var vcd = {
                    symbol,
                    name,
                    type,
                    size,
                };
                scopes[var_scope].push_back(vcd);

                if(symbols.find(symbol) == symbols.end()){
                    symbols[symbol] = {};
                }

                symbols[symbol].push_back(vcd);


            }else if(token == "$upscope"){
                while(var_scope[var_scope.length()-1] != '.') var_scope.pop_back();
                var_scope.pop_back();
            }
        }
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_frederico_vcd_MainActivity_randomValue(JNIEnv *env, jobject thiz, jint scale) {
    return random()%scale;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_frederico_vcd_MainActivity_readFile(JNIEnv *env, jobject thiz, jstring filename) {
    jboolean isCopy;
    std::string name =  env->GetStringUTFChars(filename, &isCopy);
    std::stringstream file(name);

    [[maybe_unused]]auto timescale = parser::get_timescale(file);

    std::unordered_map<std::string, std::vector<parser::VCD_var>> scopes;
    std::unordered_map<char, std::vector<parser::VCD_var>> symbols;

    parser::get_vars(file, scopes, symbols);

    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID init = env->GetMethodID(hashMapClass, "<init>", "()V");
    jobject hashmap = env->NewObject(hashMapClass, init);

    jmethodID put = env->GetMethodID(hashMapClass, "put",
                                     "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    jclass arrayList_class = env->FindClass("java/util/ArrayList");
    jmethodID array_init = env->GetMethodID(arrayList_class, "<init>", "()V");
    jmethodID add = env->GetMethodID(arrayList_class, "add", "(ILjava/lang/Object;)V");

    jclass info_class = env->FindClass("com/frederico/vcd/Info");
    jmethodID info_init = env->GetMethodID(info_class, "<init>", "(CLjava/lang/String;Ljava/lang/String;II)V");
    for(auto& [scope, vcds] : scopes){
        jstring jscope = env->NewStringUTF(scope.c_str());

        jobject arrayList = env->NewObject(arrayList_class, array_init);
        int array_size = 0;


        for(auto& vcd : vcds){
            jstring jvcd_scope = env->NewStringUTF(vcd.scope.c_str());
            jstring jvcd_type = env->NewStringUTF(vcd.type.c_str());
            jobject info =  env->NewObject(info_class, info_init, vcd.symbol, jvcd_scope, jvcd_type, vcd.size, vcd.val);
            env->CallVoidMethod(arrayList, add, array_size, info);
            array_size++;
        }

        env->CallObjectMethod(hashmap, put, jscope, arrayList);

    }

    return hashmap;
}


struct Point {
    int time;
    int value;
};
extern "C"
JNIEXPORT jobject JNICALL
Java_com_frederico_vcd_MainActivity_points(JNIEnv *env, jobject thiz, jstring filename) {
    jboolean isCopy;
    std::string name =  env->GetStringUTFChars(filename, &isCopy);
    std::stringstream file(name);
    std::string token = "";
    while(!file.eof() && token != "$dumpvars") file >> token;
    if(file.eof()) throw "File ended too soon";

    std::unordered_map<char, std::vector<Point>> points;
    std::unordered_map<char, int> mapping;

    int time = 0;
    while (!file.eof()){
        file >> token;
        if(token[0] == '$') continue;
        else if(token[0] == '#') {
            token = token.substr(1);
            time = std::stoi(token);
            for(auto& [symbol, values] : points){
                values.push_back({time, mapping[symbol]});
            }
        }
        else if(token.length() > 1) {

            char symbol;
            int value;
            if(token[0] == 'b'){
                token = token.substr(1);
                if(token.empty()) throw "NAN value as wave record";
                try {
                    value = std::stoi(token, nullptr, 2);
                    file >> symbol;
                }catch(const std::exception& e){
                    value = -1;
                    symbol = token[1];
                }
            }else if (token[0] == 'x'){ //UNDEFINED VALUE FOR VARIABLE
                value = -1;
                symbol = token[1];
            }else if (token[0] == 'z'){ // TRISTATE
                value = -2;
                symbol = token[1];
            }else{
                symbol = token[token.length()-1];
                token = token.substr(0, token.length()-1);
                value = std::stoi(token);
            }

            mapping[symbol] = value;
            if(points.find(symbol) == points.end()) points[symbol] = {};
            points[symbol].push_back({time, mapping[symbol]});

        }
    }

    jclass hashmap_class = env->FindClass("java/util/HashMap");
    jmethodID hasmap_init = env->GetMethodID(hashmap_class, "<init>", "()V");
    jmethodID put = env->GetMethodID(hashmap_class, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jobject hashmap = env->NewObject(hashmap_class, hasmap_init);

    jclass point_class = env->FindClass("com/frederico/vcd/Point");
    jmethodID point_init = env->GetMethodID(point_class, "<init>", "(II)V");

    for (auto& [symbol, values] : points){
        jobjectArray point_array = env->NewObjectArray(values.size(), point_class, nullptr);

        int index = 0;

        for (auto& point : values){
            jobject instance = env->NewObject(point_class, point_init, point.time, point.value);
            env->SetObjectArrayElement(point_array, index, instance);
            index++;
        }

        std::string ssymbol = "";
        ssymbol += symbol;
        jstring jsymbol = env->NewStringUTF(ssymbol.c_str());
        env->CallObjectMethod(hashmap, put, jsymbol, point_array);

    }

    return hashmap;

}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_frederico_vcd_MainActivity_testes(JNIEnv *env, jobject thiz) {
}