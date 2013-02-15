#ifndef AIDL_AST_H
#define AIDL_AST_H

#include <string>
#include <vector>
#include <set>
#include <stdarg.h>
#include <stdio.h>

using namespace std;

class Type;

class ASTWriter;

enum {
    PACKAGE_PRIVATE = 0x00000000,
    PUBLIC          = 0x00000001,
    PRIVATE         = 0x00000002,
    PROTECTED       = 0x00000003,
    SCOPE_MASK      = 0x00000003,

    STATIC          = 0x00000010,
    FINAL           = 0x00000020,
    ABSTRACT        = 0x00000040,

    OVERRIDE        = 0x00000100,

    ALL_MODIFIERS   = 0xffffffff
};

// Write the modifiers that are set in both mod and mask
void WriteModifiers(ASTWriter* writer, int mod, int mask);

struct ClassElement
{
    ClassElement();
    virtual ~ClassElement();

    virtual void GatherTypes(set<Type*>* types) const = 0;
    virtual void Write(ASTWriter* writer) = 0;
};

struct Expression
{
    virtual ~Expression();
    virtual void Write(ASTWriter* writer) = 0;
};

struct LiteralExpression : public Expression
{
    string value;

    LiteralExpression(const string& value);
    virtual ~LiteralExpression();
    virtual void Write(ASTWriter* writer);
};

// TODO: also escape the contents.  not needed for now
struct StringLiteralExpression : public Expression
{
    string value;

    StringLiteralExpression(const string& value);
    virtual ~StringLiteralExpression();
    virtual void Write(ASTWriter* writer);
};

struct Variable : public Expression
{
    Type* type;
    string name;
    int dimension;

    Variable();
    Variable(Type* type, const string& name);
    Variable(Type* type, const string& name, int dimension);
    virtual ~Variable();

    virtual void GatherTypes(set<Type*>* types) const;
    void WriteDeclaration(ASTWriter* writer);
    void Write(ASTWriter* writer);
};

struct FieldVariable : public Expression
{
    Expression* object;
    Type* clazz;
    string name;

    FieldVariable(Expression* object, const string& name);
    FieldVariable(Type* clazz, const string& name);
    virtual ~FieldVariable();

    void Write(ASTWriter* writer);
};

struct Field : public ClassElement
{
    string comment;
    int modifiers;
    Variable *variable;
    string value;

    Field();
    Field(int modifiers, Variable* variable);
    virtual ~Field();

    virtual void GatherTypes(set<Type*>* types) const;
    virtual void Write(ASTWriter* writer);
};

struct Statement
{
    virtual ~Statement();
    virtual void Write(ASTWriter* writer) = 0;
};

struct StatementBlock : public Statement
{
    vector<Statement*> statements;

    StatementBlock();
    virtual ~StatementBlock();
    virtual void Write(ASTWriter* writer);

    void Add(Statement* statement);
    void Add(Expression* expression);
};

struct ExpressionStatement : public Statement
{
    Expression* expression;

    ExpressionStatement(Expression* expression);
    virtual ~ExpressionStatement();
    virtual void Write(ASTWriter* writer);
};

struct Assignment : public Expression
{
    Variable* lvalue;
    Expression* rvalue;
    Type* cast;

    Assignment(Variable* lvalue, Expression* rvalue);
    Assignment(Variable* lvalue, Expression* rvalue, Type* cast);
    virtual ~Assignment();
    virtual void Write(ASTWriter* writer);
};

struct MethodCall : public Expression
{
    Expression* obj;
    Type* clazz;
    string name;
    vector<Expression*> arguments;
    vector<string> exceptions;

    MethodCall(const string& name);
    MethodCall(const string& name, int argc, ...);
    MethodCall(Expression* obj, const string& name);
    MethodCall(Type* clazz, const string& name);
    MethodCall(Expression* obj, const string& name, int argc, ...);
    MethodCall(Type* clazz, const string& name, int argc, ...);
    virtual ~MethodCall();
    virtual void Write(ASTWriter* writer);

private:
    void init(int n, va_list args);
};

struct Comparison : public Expression
{
    Expression* lvalue;
    string op;
    Expression* rvalue;

    Comparison(Expression* lvalue, const string& op, Expression* rvalue);
    virtual ~Comparison();
    virtual void Write(ASTWriter* writer);
};

struct NewExpression : public Expression
{
    Type* type;
    vector<Expression*> arguments;

    NewExpression(Type* type);
    NewExpression(Type* type, int argc, ...);
    virtual ~NewExpression();
    virtual void Write(ASTWriter* writer);

private:
    void init(int n, va_list args);
};

struct NewArrayExpression : public Expression
{
    Type* type;
    Expression* size;

    NewArrayExpression(Type* type, Expression* size);
    virtual ~NewArrayExpression();
    virtual void Write(ASTWriter* writer);
};

struct Ternary : public Expression
{
    Expression* condition;
    Expression* ifpart;
    Expression* elsepart;

    Ternary();
    Ternary(Expression* condition, Expression* ifpart, Expression* elsepart);
    virtual ~Ternary();
    virtual void Write(ASTWriter* writer);
};

struct Cast : public Expression
{
    Type* type;
    Expression* expression;

    Cast();
    Cast(Type* type, Expression* expression);
    virtual ~Cast();
    virtual void Write(ASTWriter* writer);
};

struct VariableDeclaration : public Statement
{
    Variable* lvalue;
    Type* cast;
    Expression* rvalue;

    VariableDeclaration(Variable* lvalue);
    VariableDeclaration(Variable* lvalue, Expression* rvalue, Type* cast = NULL);
    virtual ~VariableDeclaration();
    virtual void Write(ASTWriter* writer);
};

struct IfStatement : public Statement
{
    Expression* expression;
    StatementBlock* statements;
    IfStatement* elseif;

    IfStatement();
    virtual ~IfStatement();
    virtual void Write(ASTWriter* writer);
};

struct ReturnStatement : public Statement
{
    Expression* expression;

    ReturnStatement(Expression* expression);
    virtual ~ReturnStatement();
    virtual void Write(ASTWriter* writer);
};

struct TryStatement : public Statement
{
    StatementBlock* statements;

    TryStatement();
    virtual ~TryStatement();
    virtual void Write(ASTWriter* writer);
};

struct CatchStatement : public Statement
{
    StatementBlock* statements;
    Variable* exception;

    CatchStatement(Variable* exception);
    virtual ~CatchStatement();
    virtual void Write(ASTWriter* writer);
};

struct FinallyStatement : public Statement
{
    StatementBlock* statements;

    FinallyStatement();
    virtual ~FinallyStatement();
    virtual void Write(ASTWriter* writer);
};

struct Case
{
    vector<string> cases;
    StatementBlock* statements;

    Case();
    Case(const string& c);
    virtual ~Case();
    virtual void Write(ASTWriter* writer);
};

struct SwitchStatement : public Statement
{
    Expression* expression;
    vector<Case*> cases;

    SwitchStatement(Expression* expression);
    virtual ~SwitchStatement();
    virtual void Write(ASTWriter* writer);
};

struct Break : public Statement
{
    Break();
    virtual ~Break();
    virtual void Write(ASTWriter* writer);
};

struct Method : public ClassElement
{
    string comment;
    int modifiers;
    Type* returnType;
    size_t returnTypeDimension;
    string name;
    vector<Variable*> parameters;
    vector<Type*> exceptions;
    StatementBlock* statements;

    Method();
    virtual ~Method();

    virtual void GatherTypes(set<Type*>* types) const;
    virtual void Write(ASTWriter* writer);
};

struct Class : public ClassElement
{
    enum {
        CLASS,
        INTERFACE
    };

    string comment;
    int modifiers;
    int what;               // CLASS or INTERFACE
    Type* type;
    Type* extends;
    vector<Type*> interfaces;
    vector<ClassElement*> elements;

    Class();
    virtual ~Class();

    virtual void GatherTypes(set<Type*>* types) const;
    virtual void Write(ASTWriter* writer);
};

struct Document
{
    string comment;
    string package;
    string originalSrc;
    set<Type*> imports;
    vector<Class*> classes;

    Document();
    virtual ~Document();

    virtual void Write(ASTWriter* writer);
};

#endif // AIDL_AST_H
