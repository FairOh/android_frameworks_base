#include "AST.h"
#include "ASTWriter.h"
#include "Type.h"

ClassElement::ClassElement()
{
}

ClassElement::~ClassElement()
{
}

Field::Field()
    :ClassElement(),
     modifiers(0),
     variable(NULL)
{
}

Field::Field(int m, Variable* v)
    :ClassElement(),
     modifiers(m),
     variable(v)
{
}

Field::~Field()
{
}

void
Field::GatherTypes(set<Type*>* types) const
{
    types->insert(this->variable->type);
}

void
Field::Write(ASTWriter* writer)
{
    writer->WriteField(this);
}

Expression::~Expression()
{
}

LiteralExpression::LiteralExpression(const string& v)
    :value(v)
{
}

LiteralExpression::~LiteralExpression()
{
}

void
LiteralExpression::Write(ASTWriter* writer)
{
    writer->WriteLiteralExpression(this);
}

StringLiteralExpression::StringLiteralExpression(const string& v)
    :value(v)
{
}

StringLiteralExpression::~StringLiteralExpression()
{
}

void
StringLiteralExpression::Write(ASTWriter* writer)
{
    writer->WriteStringLiteralExpression(this);
}

Variable::Variable()
    :type(NULL),
     name(),
     dimension(0)
{
}

Variable::Variable(Type* t, const string& n)
    :type(t),
     name(n),
     dimension(0)
{
}

Variable::Variable(Type* t, const string& n, int d)
    :type(t),
     name(n),
     dimension(d)
{
}

Variable::~Variable()
{
}

void
Variable::GatherTypes(set<Type*>* types) const
{
    types->insert(this->type);
}

void
Variable::WriteDeclaration(ASTWriter* writer)
{
    writer->WriteVariableDeclaration(this);
}

void
Variable::Write(ASTWriter* writer)
{
    writer->WriteVariable(this);
}

FieldVariable::FieldVariable(Expression* o, const string& n)
    :object(o),
     clazz(NULL),
     name(n)
{
}

FieldVariable::FieldVariable(Type* c, const string& n)
    :object(NULL),
     clazz(c),
     name(n)
{
}

FieldVariable::~FieldVariable()
{
}

void
FieldVariable::Write(ASTWriter* writer)
{
    writer->WriteFieldVariable(this);
}


Statement::~Statement()
{
}

StatementBlock::StatementBlock()
{
}

StatementBlock::~StatementBlock()
{
}

void
StatementBlock::Write(ASTWriter* writer)
{
    writer->WriteStatementBlock(this);
}

void
StatementBlock::Add(Statement* statement)
{
    this->statements.push_back(statement);
}

void
StatementBlock::Add(Expression* expression)
{
    this->statements.push_back(new ExpressionStatement(expression));
}

ExpressionStatement::ExpressionStatement(Expression* e)
    :expression(e)
{
}

ExpressionStatement::~ExpressionStatement()
{
}

void
ExpressionStatement::Write(ASTWriter* writer)
{
    writer->WriteExpressionStatement(this);
}

Assignment::Assignment(Variable* l, Expression* r)
    :lvalue(l),
     rvalue(r),
     cast(NULL)
{
}

Assignment::Assignment(Variable* l, Expression* r, Type* c)
    :lvalue(l),
     rvalue(r),
     cast(c)
{
}

Assignment::~Assignment()
{
}

void
Assignment::Write(ASTWriter* writer)
{
    writer->WriteAssignment(this);
}

MethodCall::MethodCall(const string& n)
    :obj(NULL),
     clazz(NULL),
     name(n)
{
}

MethodCall::MethodCall(const string& n, int argc = 0, ...)
    :obj(NULL),
     clazz(NULL),
     name(n)
{
  va_list args;
  va_start(args, argc);
  init(argc, args);
  va_end(args);
}

MethodCall::MethodCall(Expression* o, const string& n)
    :obj(o),
     clazz(NULL),
     name(n)
{
}

MethodCall::MethodCall(Type* t, const string& n)
    :obj(NULL),
     clazz(t),
     name(n)
{
}

MethodCall::MethodCall(Expression* o, const string& n, int argc = 0, ...)
    :obj(o),
     clazz(NULL),
     name(n)
{
  va_list args;
  va_start(args, argc);
  init(argc, args);
  va_end(args);
}

MethodCall::MethodCall(Type* t, const string& n, int argc = 0, ...)
    :obj(NULL),
     clazz(t),
     name(n)
{
  va_list args;
  va_start(args, argc);
  init(argc, args);
  va_end(args);
}

MethodCall::~MethodCall()
{
}

void
MethodCall::init(int n, va_list args)
{
    for (int i=0; i<n; i++) {
        Expression* expression = (Expression*)va_arg(args, void*);
        this->arguments.push_back(expression);
    }
}

void
MethodCall::Write(ASTWriter* writer)
{
    writer->WriteMethodCall(this);
}

Comparison::Comparison(Expression* l, const string& o, Expression* r)
    :lvalue(l),
     op(o),
     rvalue(r)
{
}

Comparison::~Comparison()
{
}

void
Comparison::Write(ASTWriter* writer)
{
    writer->WriteComparison(this);
}

NewExpression::NewExpression(Type* t)
    :type(t)
{
}

NewExpression::NewExpression(Type* t, int argc = 0, ...)
    :type(t)
{
  va_list args;
  va_start(args, argc);
  init(argc, args);
  va_end(args);
}

NewExpression::~NewExpression()
{
}

void
NewExpression::init(int n, va_list args)
{
    for (int i=0; i<n; i++) {
        Expression* expression = (Expression*)va_arg(args, void*);
        this->arguments.push_back(expression);
    }
}

void
NewExpression::Write(ASTWriter* writer)
{
    writer->WriteNewExpression(this);
}

NewArrayExpression::NewArrayExpression(Type* t, Expression* s)
    :type(t),
     size(s)
{
}

NewArrayExpression::~NewArrayExpression()
{
}

void
NewArrayExpression::Write(ASTWriter* writer)
{
    writer->WriteNewArrayExpression(this);
}

Ternary::Ternary()
    :condition(NULL),
     ifpart(NULL),
     elsepart(NULL)
{
}

Ternary::Ternary(Expression* a, Expression* b, Expression* c)
    :condition(a),
     ifpart(b),
     elsepart(c)
{
}

Ternary::~Ternary()
{
}

void
Ternary::Write(ASTWriter* writer)
{
    writer->WriteTernary(this);
}

Cast::Cast()
    :type(NULL),
     expression(NULL)
{
}

Cast::Cast(Type* t, Expression* e)
    :type(t),
     expression(e)
{
}

Cast::~Cast()
{
}

void
Cast::Write(ASTWriter* writer)
{
    writer->WriteCast(this);
}

VariableDeclaration::VariableDeclaration(Variable* l, Expression* r, Type* c)
    :lvalue(l),
     cast(c),
     rvalue(r)
{
}

VariableDeclaration::VariableDeclaration(Variable* l)
    :lvalue(l),
     cast(NULL),
     rvalue(NULL)
{
}

VariableDeclaration::~VariableDeclaration()
{
}

void
VariableDeclaration::Write(ASTWriter* writer)
{
    writer->WriteVariableDeclaration(this);
}

IfStatement::IfStatement()
    :expression(NULL),
     statements(new StatementBlock),
     elseif(NULL)
{
}

IfStatement::~IfStatement()
{
}

void
IfStatement::Write(ASTWriter* writer)
{
    writer->WriteIfStatement(this);
}

ReturnStatement::ReturnStatement(Expression* e)
    :expression(e)
{
}

ReturnStatement::~ReturnStatement()
{
}

void
ReturnStatement::Write(ASTWriter* writer)
{
    writer->WriteReturnStatement(this);
}

TryStatement::TryStatement()
    :statements(new StatementBlock)
{
}

TryStatement::~TryStatement()
{
}

void
TryStatement::Write(ASTWriter* writer)
{
    writer->WriteTryStatement(this);
}

CatchStatement::CatchStatement(Variable* e)
    :statements(new StatementBlock),
     exception(e)
{
}

CatchStatement::~CatchStatement()
{
}

void
CatchStatement::Write(ASTWriter* writer)
{
    writer->WriteCatchStatement(this);
}

FinallyStatement::FinallyStatement()
    :statements(new StatementBlock)
{
}

FinallyStatement::~FinallyStatement()
{
}

void
FinallyStatement::Write(ASTWriter* writer)
{
    writer->WriteFinallyStatement(this);
}

Case::Case()
    :statements(new StatementBlock)
{
}

Case::Case(const string& c)
    :statements(new StatementBlock)
{
    cases.push_back(c);
}

Case::~Case()
{
}

void
Case::Write(ASTWriter* writer)
{
    writer->WriteCase(this);
}

SwitchStatement::SwitchStatement(Expression* e)
    :expression(e)
{
}

SwitchStatement::~SwitchStatement()
{
}

void
SwitchStatement::Write(ASTWriter* writer)
{
    writer->WriteSwitchStatement(this);
}

Break::Break()
{
}

Break::~Break()
{
}

void
Break::Write(ASTWriter* writer)
{
    writer->WriteBreak(this);
}

Method::Method()
    :ClassElement(),
     modifiers(0),
     returnType(NULL), // (NULL means constructor)
     returnTypeDimension(0),
     statements(NULL)
{
}

Method::~Method()
{
}

void
Method::GatherTypes(set<Type*>* types) const
{
    size_t N, i;

    if (this->returnType) {
        types->insert(this->returnType);
    }

    N = this->parameters.size();
    for (i=0; i<N; i++) {
        this->parameters[i]->GatherTypes(types);
    }

    N = this->exceptions.size();
    for (i=0; i<N; i++) {
        types->insert(this->exceptions[i]);
    }
}

void
Method::Write(ASTWriter* writer)
{
    writer->WriteMethod(this);
}

Class::Class()
    :modifiers(0),
     what(CLASS),
     type(NULL),
     extends(NULL)
{
}

Class::~Class()
{
}

void
Class::GatherTypes(set<Type*>* types) const
{
    int N, i;

    types->insert(this->type);
    if (this->extends != NULL) {
        types->insert(this->extends);
    }

    N = this->interfaces.size();
    for (i=0; i<N; i++) {
        types->insert(this->interfaces[i]);
    }

    N = this->elements.size();
    for (i=0; i<N; i++) {
        this->elements[i]->GatherTypes(types);
    }
}

void
Class::Write(ASTWriter* writer)
{
    writer->WriteClass(this);
}

Document::Document()
{
}

Document::~Document()
{
}

void
Document::Write(ASTWriter* writer)
{
    writer->WriteDocument(this);
}

